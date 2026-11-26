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
    var stage by remember { mutableStateOf("") }
    val byRole = clients.filter { it.role == if (role == "Покупатели") "buyer" else "owner" }
    val list = byRole.filter { stage.isEmpty() || it.stage == stage }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Title("Воронка продаж") }
        item {
            val filters = listOf("" to "Все") + stages
            val labels = filters.associate { (key, label) ->
                key to "$label · ${byRole.count { key.isEmpty() || it.stage == key }}"
            }
            Chips(labels.values.toList(), labels.getValue(stage)) { selected ->
                stage = labels.entries.first { it.value == selected }.key
            }
        }
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
                    Text(stageName(c.stage), color = Lime, style = MaterialTheme.typography.labelMedium)
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
        if (c.role == "buyer") item {
            var editCriteria by remember { mutableStateOf(false) }
            AppCard {
                Text("Требования к квартире", style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(c.searchArea, c.wishes, when(c.preferredMarket) { "primary" -> "Первичка"; "secondary" -> "Вторичка"; else -> null }, c.desiredRooms.takeIf { it > 0 }?.let { "$it комн." },
                    c.minArea.takeIf { it > 0 }?.let { "от $it м²" }).joinToString(" · ").ifBlank { "Все районы и планировки" }, color = Muted)
                TextButton({ editCriteria = true }) { Text("Изменить подбор") }
            }
            if (editCriteria) {
                var area by remember { mutableStateOf(c.searchArea.orEmpty()) }
                var rooms by remember { mutableStateOf(c.desiredRooms.takeIf { it > 0 }?.toString().orEmpty()) }
                var minimum by remember { mutableStateOf(c.minArea.takeIf { it > 0 }?.toString().orEmpty()) }
                var market by remember { mutableStateOf(c.preferredMarket) }
                var wishes by remember { mutableStateOf(c.wishes.orEmpty()) }
                val size = minimum.replace(',', '.').toDoubleOrNull()
                AlertDialog(onDismissRequest = { editCriteria = false }, title = { Text("Критерии подбора") },
                    text = { Column {
                        val markets = mapOf("Любой" to null, "Первичка" to "primary", "Вторичка" to "secondary")
                        Chips(markets.keys.toList(), markets.entries.firstOrNull { it.value == market }?.key ?: "Любой") { market = markets[it] }
                        Field(wishes, { wishes = it }, "Пожелания через запятую")
                        Field(area, { area = it }, "Район или улица в адресе")
                        Field(rooms, { rooms = it.filter(Char::isDigit).take(2) }, "Количество комнат", keyboard = KeyboardType.Number)
                        Field(minimum, { minimum = it }, "Минимальная площадь, м²", keyboard = KeyboardType.Decimal)
                    } }, confirmButton = { Button(enabled = minimum.isBlank() || (size != null && size.isFinite() && size >= 0), onClick = {
                        vm.saveClient(c.copy(preferredMarket = market, wishes = wishes.trim().ifBlank { null }, searchArea = area.trim().ifBlank { null }, desiredRooms = rooms.toIntOrNull() ?: 0, minArea = size ?: 0.0))
                        editCriteria = false
                    }) { Text("Сохранить") } })
            }
        }
        if (c.role == "buyer") {
            val picks = ru.vladis.realtorai.autopilot.AutoRules.matches(c, props)
            if (picks.isNotEmpty()) item {
                val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                AppCard {
                    Text("Предложение клиенту", style = MaterialTheme.typography.titleMedium)
                    val draft = "Здравствуйте, ${c.name}! Подобрал варианты по вашему запросу:\n" +
                        picks.joinToString("\n") { "• ${it.title} · ${it.address} · ${it.price.rub()}" } +
                        "\nКакие из них хотите посмотреть?"
                    Text(draft)
                    TextButton({ clip.setText(androidx.compose.ui.text.AnnotatedString(draft)) }) { Text("Скопировать черновик") }
                }
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
        item { Text("Автопилот добавляет встречу в календарь из записи вида «Показ 30.11.2026 в 15:00».", color = Muted, style = MaterialTheme.typography.bodySmall) }
        items(history, key = { it.id }) { h -> AppCard { Text(fmtDate(h.at), color = Muted, style = MaterialTheme.typography.bodySmall); Text(h.text) } }
        item { TextButton({ vm.deleteClient(c); back() }) { Text("Удалить клиента", color = MaterialTheme.colorScheme.error) } }
    }
}
