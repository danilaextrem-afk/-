package ru.vladis.realtorai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.Client

val stages = listOf("new" to "Новый", "contact" to "Контакт", "showing" to "Показ", "deposit" to "Задаток", "deal" to "Сделка", "lost" to "Отказ")
fun stageName(k: String) = stages.firstOrNull { it.first == k }?.second ?: k

@Composable fun Crm(vm: MainVm, open: (Long) -> Unit) {
    val clients by vm.clients.collectAsState()
    var role by remember { mutableStateOf("Покупатели") }
    var adding by remember { mutableStateOf(false) }
    val list = clients.filter { it.role == if (role == "Покупатели") "buyer" else "owner" }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Title("CRM") }
        item { Chips(listOf("Покупатели", "Собственники"), role) { role = it } }
        item { Button({ adding = true }) { Text("+ Клиент") } }
        if (list.isEmpty()) item { Text("Список пуст", color = Muted) }
        items(list, key = { it.id }) { c ->
            AppCard(onClick = { open(c.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.SemiBold)
                        if (c.phone.isNotBlank()) Text(c.phone, color = Muted)
                        if (c.budget > 0) Text("Бюджет: ${c.budget.rub()}", color = Muted)
                    }
                    AssistChip({}, label = { Text(stageName(c.stage)) })
                }
            }
        }
    }
    if (adding) {
        var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
        var budget by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
        var r by remember { mutableStateOf(role) }
        AlertDialog(onDismissRequest = { adding = false }, title = { Text("Новый клиент") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chips(listOf("Покупатели", "Собственники"), r) { r = it }
                Field(name, { name = it }, "Имя")
                Field(phone, { phone = it }, "Телефон", keyboard = KeyboardType.Phone)
                Field(budget, { budget = it.filter(Char::isDigit) }, "Бюджет, ₽", keyboard = KeyboardType.Number)
                Field(notes, { notes = it }, "Заметки", single = false)
            }
        }, confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                vm.saveClient(Client(name = name.trim(), phone = phone.trim(), role = if (r == "Покупатели") "buyer" else "owner",
                    budget = budget.toLongOrNull() ?: 0, notes = notes)); adding = false
            }) { Text("Сохранить") }
        }, dismissButton = { TextButton({ adding = false }) { Text("Отмена") } })
    }
}

@Composable fun ClientDetail(vm: MainVm, id: Long, back: () -> Unit) {
    val clients by vm.clients.collectAsState()
    val props by vm.properties.collectAsState()
    val history by vm.interactions(id).collectAsState(emptyList())
    val ctx = LocalContext.current
    val c = clients.find { it.id == id }
    if (c == null) { LaunchedEffect(Unit) { back() }; return }
    var note by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }; Title(c.name) } }
        item {
            AppCard {
                Text(if (c.role == "buyer") "Покупатель" else "Собственник", color = Muted)
                if (c.phone.isNotBlank()) TextButton({ ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + c.phone))) }) { Text("Позвонить: ${c.phone}") }
                if (c.budget > 0) Text("Бюджет: ${c.budget.rub()}")
                if (c.notes.isNotBlank()) Text(c.notes, color = Muted)
            }
        }
        item { Text("Статус сделки", color = Muted); Chips(stages.map { it.second }, stageName(c.stage)) { n -> vm.saveClient(c.copy(stage = stages.first { it.second == n }.first)) } }
        if (c.role == "owner") {
            val own = props.filter { it.ownerId == c.id }
            if (own.isNotEmpty()) item { AppCard { Text("Объекты", fontWeight = FontWeight.SemiBold); own.forEach { Text("• ${it.title} — ${it.price.rub()}", color = Muted) } } }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(note, { note = it }, "Запись в историю общения", Modifier.weight(1f))
                IconButton(enabled = note.isNotBlank(), onClick = { vm.addInteraction(c.id, note.trim()); note = "" }) { Icon(Icons.Default.Send, "Добавить", tint = Lime) }
            }
        }
        items(history, key = { it.id }) { h -> AppCard { Text(fmtDate(h.at), color = Muted, style = MaterialTheme.typography.bodySmall); Text(h.text) } }
        item { TextButton({ vm.deleteClient(c); back() }) { Text("Удалить клиента", color = MaterialTheme.colorScheme.error) } }
    }
}
