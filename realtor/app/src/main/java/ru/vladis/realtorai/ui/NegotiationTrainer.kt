package ru.vladis.realtorai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.vladis.realtorai.MainVm

@Composable
internal fun NegotiationTrainer(vm: MainVm, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scenarios = listOf("Продаёт сам", "Уже работает с агентом", "Возражает против комиссии", "Не готов снижать цену")
    var scenario by remember { mutableStateOf(scenarios.first()) }
    var turns by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var draft by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun run(review: Boolean) {
        val messages = if (review) turns + ("user" to "Разбери этот учебный разговор.")
            else turns + ("user" to draft.trim())
        busy = true
        failure = null
        scope.launch {
            try {
                val answer = vm.rolePlay(scenario, messages.takeLast(12), review)
                if (review) feedback = answer else {
                    turns = messages + ("assistant" to answer)
                    draft = ""
                    feedback = ""
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure = e.message ?: "Не удалось получить ответ" }
            finally { busy = false }
        }
    }
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(back) { Text("Назад") }
            ScreenHeading("Тренажёр переговоров", "Учебный звонок виртуальному собственнику")
            Chips(scenarios, scenario) { next ->
                if (!busy && next != scenario) {
                    scenario = next
                    turns = emptyList()
                    feedback = ""
                }
            }
        }
        items(turns) { (role, message) ->
            AppCard {
                Text(if (role == "user") "Вы" else "Виртуальный собственник", color = Lime, style = MaterialTheme.typography.labelMedium)
                Text(message)
            }
        }
        item {
            Field(draft, { draft = it.take(800) }, "Ваша реплика", single = false)
            Row {
                Button({ run(false) }, enabled = !busy && draft.isNotBlank()) { Text("Ответить") }
                TextButton({ run(true) }, enabled = !busy && turns.isNotEmpty()) { Text("Разбор") }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (feedback.isNotBlank()) AppCard { Text("Разбор", color = Lime); Text(feedback) }
        }
    }
}
