package ru.vladis.realtorai.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.WorkspaceDb
import ru.vladis.realtorai.ui.*

@Composable
fun AgentScreen(vm: MainVm, back: () -> Unit) {
    val ctx = LocalContext.current.applicationContext
    val queue = remember(ctx) { AgentQueue(ctx) }
    val flow = remember(ctx) { WorkspaceDb.get(ctx).agent().recent() }
    val jobs by flow.collectAsState(emptyList())
    val buyers by vm.clients.collectAsState()
    val scope = rememberCoroutineScope()
    var goal by remember { mutableStateOf("") }
    var client by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var proposed by remember(goal) { mutableStateOf<List<AgentTool>?>(null) }
    var planning by remember { mutableStateOf(false) }
    val plan = proposed ?: remember(goal) { AgentPlan.fromGoal(goal) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(back) { Text("Назад") }
            Title("Поручения агенту")
        }
        item {
            AppCard {
                Field(goal, { goal = it.take(500) }, "Что поручить агенту?", single = false)
                VoiceInput({ goal = (goal + " " + it).trim().take(500) }, { error = it })
                Chips(listOf("Найди выгодные квартиры", "Подбери под бюджет клиента", "Составь план на неделю"), "") { goal = it }
                if (vm.deviceReady) OutlinedButton({
                    val request = goal.trim()
                    planning = true
                    scope.launch {
                        try {
                            val answer = ru.vladis.realtorai.ai.DeviceModel.answer(ctx,
                                "Верни только до 4 кодов инструментов через запятую без пояснений. FEEDS поиск по источникам и импорт, MATCH подбор покупателю, MARKET цены, LEADS импортированные контакты, WEEK план недели, OWNERS обновить сроки отчётов собственникам. Используй только этот список. Команд отправки и публикации нет.",
                                listOf("user" to request))
                            if (request == goal.trim()) proposed = AgentPlan.fromModel(answer)
                            error = null
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = e.localizedMessage }
                        finally { planning = false }
                    }
                }, enabled = goal.isNotBlank() && !planning && !submitting) {
                    Text(if (planning) "Модель планирует…" else "Составить план локальной моделью")
                }
                if (AgentTool.MATCH in plan) Box {
                    OutlinedButton({ menu = true }) { Text(buyers.firstOrNull { it.id == client }?.name ?: "Выбрать покупателя") }
                    DropdownMenu(menu, { menu = false }) {
                        buyers.filter { it.role == "buyer" }.forEach { buyer ->
                            DropdownMenuItem(text = { Text(buyer.name) }, onClick = { client = buyer.id; menu = false })
                        }
                    }
                }
                if (plan.isEmpty() && goal.isNotBlank()) Text("Попробуйте поиск, цены, подбор, контакты, план недели или сопровождение.", color = Muted)
                plan.forEachIndexed { i, tool -> Text("${i + 1}. ${tool.label}") }
                if (plan.any { it.network }) Text("Поручение использует существующие настройки Deal Hunter и сохраняет результаты в CRM.", color = Muted)
                Button({
                    val request = goal.trim()
                    val selected = client
                    submitting = true
                    scope.launch {
                        try { queue.submit(request, plan, selected); goal = ""; error = null }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = e.localizedMessage }
                        finally { submitting = false }
                    }
                }, enabled = !submitting && !planning && plan.isNotEmpty() && (AgentTool.MATCH !in plan || client != null)) { Text("Запустить план") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item { Title("Журнал поручений") }
        if (jobs.isEmpty()) item { Text("Здесь появятся результаты и ошибки поручений.", color = Muted) }
        items(jobs, key = { it.id }) { job ->
            AppCard {
                Text(job.goal, style = MaterialTheme.typography.titleLarge)
                Text((mapOf("queued" to "В очереди", "running" to "Выполняется", "done" to "Выполнено",
                    "failed" to "Ошибка", "cancelled" to "Отменено")[job.state] ?: job.state) + " · " + fmtDate(job.createdAt), color = Lime)
                if (job.result.isNotBlank()) SelectionContainer { Text(job.result) }
                if (job.state in listOf("queued", "running")) TextButton({ scope.launch { queue.cancel(job) } }) { Text("Отменить") }
                if (job.state in listOf("failed", "cancelled")) TextButton({ scope.launch { queue.resume(job) } }) { Text("Повторить") }
            }
        }
    }
}
