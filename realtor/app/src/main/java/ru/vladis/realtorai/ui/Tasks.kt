package ru.vladis.realtorai.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val context = LocalContext.current
    var notifications by remember {
        mutableStateOf(android.os.Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { notifications = it }
    val tasks by vm.tasks.collectAsState()
    var filter by remember { mutableStateOf("Предстоящие") }
    var adding by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var week by remember { mutableStateOf(today.minusDays(today.dayOfWeek.value.toLong() - 1)) }
    val staged = when (filter) { "Предстоящие" -> tasks.filter { !it.done }; "Встречи" -> tasks.filter { (it.kind == "meeting" || it.kind.startsWith("meeting/")) && !it.done }; else -> tasks.filter { it.done } }
    val shown = staged.filter { selected == null || Instant.ofEpochMilli(it.dueAt).atZone(zone).toLocalDate() == selected }
    val groups = shown.sortedBy { it.dueAt }.groupBy { headFmt.format(Date(it.dueAt)) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenHeading("Задачи и встречи", "Календарь риелтора") {
                FilledTonalIconButton({ adding = true }) { Icon(Icons.Default.Add, "Новая задача") }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = Surface1) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ week = week.minusWeeks(1) }) { Icon(Icons.Default.ArrowBack, "Предыдущая неделя") }
                        Text(week.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))).replaceFirstChar(Char::uppercase),
                            Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconButton({ week = week.plusWeeks(1) }) { Icon(Icons.Default.ArrowForward, "Следующая неделя") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 0..6) {
                            val day = week.plusDays(i.toLong())
                            val count = staged.count { Instant.ofEpochMilli(it.dueAt).atZone(zone).toLocalDate() == day }
                            Surface(onClick = { selected = if (selected == day) null else day }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                color = if (selected == day) MaterialTheme.colorScheme.primaryContainer else Surface1) {
                                Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(day.format(DateTimeFormatter.ofPattern("EE", Locale.forLanguageTag("ru"))), color = Muted, style = MaterialTheme.typography.bodySmall)
                                    Text(day.dayOfMonth.toString(), color = if (day == today || selected == day) Lime else MaterialTheme.colorScheme.onSurface)
                                    Text(if (count > 0) count.toString() else " ", color = Lime, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    if (selected != null) TextButton({ selected = null }) { Text("Показать все даты") }
                }
            }
        }
        if (!notifications && android.os.Build.VERSION.SDK_INT >= 33) item {
            TextButton({ permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { Text("Включить напоминания") }
        }
        item { Chips(listOf("Предстоящие", "Встречи", "Выполнено"), filter) { filter = it } }
        if (shown.isEmpty()) item {
            HorizontalDivider()
            Text(when (filter) {
                "Встречи" -> "Встречи пока не назначены"
                "Выполнено" -> "Завершённые задачи появятся здесь"
                else -> "Нет запланированных задач"
            }, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleLarge)
            if (filter != "Выполнено") TextButton({ adding = true }, contentPadding = PaddingValues(0.dp)) { Text("Добавить запись") }
        }
        groups.forEach { (day, list) ->
            item { Text(day.replaceFirstChar { it.uppercase() }, color = Lime, style = MaterialTheme.typography.titleSmall) }
            items(list.size) { i ->
                val t = list[i]
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(timeFmt.format(Date(t.dueAt)), Modifier.width(48.dp),
                            color = if (!t.done && t.dueAt < now) MaterialTheme.colorScheme.error else Lime,
                            style = MaterialTheme.typography.labelMedium)
                        Checkbox(t.done, { vm.saveTask(t.copy(done = it)) })
                        Column(Modifier.weight(1f)) {
                            Text(t.title, textDecoration = if (t.done) TextDecoration.LineThrough else null)
                            Text(if (!t.done && t.dueAt < now) "Просрочено" else if ((t.kind == "meeting" || t.kind.startsWith("meeting/"))) "Встреча" else "Задача",
                                color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ vm.deleteTask(t) }) { Icon(Icons.Default.Delete, "Удалить", tint = Muted) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
