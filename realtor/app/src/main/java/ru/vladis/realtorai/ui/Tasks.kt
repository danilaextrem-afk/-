package ru.vladis.realtorai.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.TaskItem
import java.text.SimpleDateFormat
import java.util.*

private val headFmt = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
private val timeFmt = SimpleDateFormat("HH:mm", Locale("ru"))

/** Календарь в виде повестки: задачи и встречи сгруппированы по дням. */
@Composable fun Tasks(vm: MainVm) {
    val tasks by vm.tasks.collectAsState()
    var filter by remember { mutableStateOf("Предстоящие") }
    var adding by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val shown = when (filter) { "Предстоящие" -> tasks.filter { !it.done }; "Встречи" -> tasks.filter { it.kind == "meeting" && !it.done }; else -> tasks.filter { it.done } }
    val groups = shown.groupBy { headFmt.format(Date(it.dueAt)) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Title("Задачи и встречи") }
        item { Chips(listOf("Предстоящие", "Встречи", "Выполнено"), filter) { filter = it } }
        item { Button({ adding = true }) { Text("+ Задача / встреча") } }
        if (shown.isEmpty()) item { Text("Пусто", color = Muted) }
        groups.forEach { (day, list) ->
            item { Text(day.replaceFirstChar { it.uppercase() }, color = Lime, style = MaterialTheme.typography.titleSmall) }
            items(list.size) { i ->
                val t = list[i]
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(t.done, { vm.saveTask(t.copy(done = it)) })
                        Column(Modifier.weight(1f)) {
                            Text(t.title, textDecoration = if (t.done) TextDecoration.LineThrough else null)
                            Text((if (t.kind == "meeting") "Встреча · " else "") + timeFmt.format(Date(t.dueAt)) + if (!t.done && t.dueAt < now) " · просрочено" else "", color = Muted)
                        }
                        IconButton({ vm.deleteTask(t) }) { Icon(Icons.Default.Delete, "Удалить", tint = Muted) }
                    }
                }
            }
        }
    }
    if (adding) {
        val ctx = LocalContext.current
        var title by remember { mutableStateOf("") }
        var kind by remember { mutableStateOf("Задача") }
        var at by remember { mutableLongStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }.timeInMillis) }
        AlertDialog(onDismissRequest = { adding = false }, title = { Text("Новая запись") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(title, { title = it }, "Название")
                Chips(listOf("Задача", "Встреча"), kind) { kind = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({
                        val c = Calendar.getInstance().apply { timeInMillis = at }
                        DatePickerDialog(ctx, { _, y, m, d -> at = Calendar.getInstance().apply { timeInMillis = at; set(y, m, d) }.timeInMillis },
                            c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH]).show()
                    }) { Text(SimpleDateFormat("d MMM yyyy", Locale("ru")).format(Date(at))) }
                    OutlinedButton({
                        val c = Calendar.getInstance().apply { timeInMillis = at }
                        TimePickerDialog(ctx, { _, h, mi -> at = Calendar.getInstance().apply { timeInMillis = at; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, mi) }.timeInMillis },
                            c[Calendar.HOUR_OF_DAY], c[Calendar.MINUTE], true).show()
                    }) { Text(timeFmt.format(Date(at))) }
                }
                Text("Напоминание придёт за 15 минут", color = Muted)
            }
        }, confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = {
                vm.saveTask(TaskItem(title = title.trim(), kind = if (kind == "Встреча") "meeting" else "task", dueAt = at)); adding = false
            }) { Text("Сохранить") }
        }, dismissButton = { TextButton({ adding = false }) { Text("Отмена") } })
    }
}
