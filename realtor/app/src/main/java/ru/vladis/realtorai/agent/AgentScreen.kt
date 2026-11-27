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
fun AgentScreen(vm: MainVm, initialGoal: String, back: () -> Unit) {
    val ctx = LocalContext.current.applicationContext
    val queue = remember(ctx) { AgentQueue(ctx) }
    val flow = remember(ctx) { WorkspaceDb.get(ctx).agent().recent() }
    val jobs by flow.collectAsState(emptyList())
    val buyers by vm.clients.collectAsState()
    val scope = rememberCoroutineScope()
    var goal by remember(initialGoal) { mutableStateOf(initialGoal) }
    var client by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val plan = remember(goal) { AgentPlan.fromGoal(goal) }
    var answerTo by remember { mutableStateOf<AgentJob?>(null) }
    var reply by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(back) { Text("Назад") }
            ScreenHeading("Поручения агенту", "Поиск, CRM, отчёты и рабочие задачи")
        }
        item {
            AppCard {
                Field(goal, { goal = it.take(500) }, "Что поручить агенту?", single = false)
                if (vm.cloudReady) Text("GPT-OSS 120B: в Groq отправляется поручение и коды выполненных инструментов, без карточек CRM.", color = Muted)
                VoiceInput({ goal = (goal + " " + it).trim().take(500) }, { error = it })
                Chips(listOf("Составь план на неделю", "Найди выгодные квартиры", "Подбери под бюджет клиента"), "") { goal = it }
                if (buyers.any { it.role == "buyer" }) Box {
                    OutlinedButton({ menu = true }) { Text(buyers.firstOrNull { it.id == client }?.name ?: "Выбрать покупателя") }
                    DropdownMenu(menu, { menu = false }) {
                        buyers.filter { it.role == "buyer" }.forEach { buyer ->
                            DropdownMenuItem(text = { Text(buyer.name) }, onClick = { client = buyer.id; menu = false })
                        }
                    }
                }
                if (!vm.deviceReady && !vm.cloudReady && plan.isEmpty() && goal.isNotBlank()) Text("Попробуйте поиск, цены, подбор, контакты, план недели или сопровождение.", color = Muted)
                if (!vm.deviceReady && !vm.cloudReady) plan.forEachIndexed { i, tool -> Text("${i + 1}. ${tool.label}") }
                if (plan.any { it.network }) Text("Поручение использует существующие настройки Deal Hunter и сохраняет результаты в CRM.", color = Muted)
                Button({
                    val request = goal.trim()
                    val selected = client
                    submitting = true
                    scope.launch {
                        try {
                            if (vm.deviceReady || vm.cloudReady) queue.adaptive(request, selected) else queue.submit(request, plan, selected)
                            goal = ""; error = null
                        }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = e.localizedMessage }
                        finally { submitting = false }
                    }
                }, enabled = !submitting && goal.isNotBlank() && (vm.deviceReady || vm.cloudReady || plan.isNotEmpty() && (AgentTool.MATCH !in plan || client != null))) { Text("Дать поручение") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item { Title("Журнал поручений") }
        if (jobs.isEmpty()) item { Text("Здесь появятся результаты и ошибки поручений.", color = Muted) }
        items(jobs, key = { it.id }) { job ->
            AppCard {
                Text(job.goal, style = MaterialTheme.typography.titleLarge)
                Text((mapOf("queued" to "В очереди", "running" to "Выполняется", "done" to "Выполнено",
                    "waiting" to "Нужно уточнение", "failed" to "Ошибка", "cancelled" to "Отменено")[job.state] ?: job.state) + " · " + fmtDate(job.createdAt), color = Lime)
                if (job.state == "running" && job.result.isBlank()) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (job.result.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    SelectionContainer { Text(job.result, style = MaterialTheme.typography.bodyMedium) }
                }
                if (job.state == "waiting") TextButton({ answerTo = job; reply = "" }) { Text("Ответить агенту") }
                if (job.state in listOf("queued", "running")) TextButton({ scope.launch { queue.cancel(job) } }) { Text("Отменить") }
                if (job.state in listOf("failed", "cancelled")) TextButton({ scope.launch { queue.resume(job) } }) { Text("Продолжить") }
            }
        }
    }
    answerTo?.let { job ->
        AlertDialog(onDismissRequest = { answerTo = null }, title = { Text("Уточнение") },
            text = { Field(reply, { reply = it.take(350) }, "Ответ", single = false) },
            confirmButton = { Button({ scope.launch { queue.answer(job, reply); answerTo = null } }, enabled = reply.isNotBlank()) { Text("Продолжить") } })
    }
}
