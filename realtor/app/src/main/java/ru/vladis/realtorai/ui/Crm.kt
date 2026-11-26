package ru.vladis.realtorai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
        item {
            ScreenHeading("Воронка продаж", "${clients.size} контактов в CRM") {
                FilledTonalButton({ adding = true }) { Text("+ Клиент") }
            }
            Chips(listOf("Покупатели", "Собственники"), role) { role = it }
            val frequencies = byRole.groupingBy { it.stage }.eachCount()
            val maximum = frequencies.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            Surface(shape = MaterialTheme.shapes.large, color = Surface1) {
                Row(Modifier.fillMaxWidth().height(128.dp).padding(9.dp), horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom) {
                    stages.forEach { (key, title) ->
                        val count = frequencies.getOrDefault(key, 0)
                        Column(Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable {
                            stage = if (stage == key) "" else key
                        }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(count.toString(), color = if (stage == key) Lime else Muted)
                            Surface(Modifier.fillMaxWidth().height((4 + 56 * count / maximum).dp),
                                shape = MaterialTheme.shapes.small, color = if (count > 0 || stage == key) Lime else Surface2) {}
                            Text(title, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (stage.isNotEmpty()) TextButton({ stage = "" }) { Text("Сбросить этап · ${stageName(stage)}") }
        }
        item { ScreenHeading("Контакты", if (stage.isEmpty()) role else stageName(stage)) }
        if (list.isEmpty()) item {
            Text(if (byRole.isEmpty()) "Добавьте первый контакт или импортируйте его через раздел «Лиды»."
                else "На этом этапе пока нет контактов.", color = Muted)
        }
        items(list, key = { it.id }) { c ->
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { open(c.id) }.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Surface(Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) { Text(c.name.trim().take(2).uppercase(), color = Lime) }
                }
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium)
                    Text(listOfNotNull(c.phone.takeIf(String::isNotBlank), c.budget.takeIf { it > 0 }?.rub()).joinToString(" · ").ifEmpty { "Данные уточняются" }, color = Muted)
                }
                Text(stageName(c.stage), color = Lime, style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        item {
            IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Surface(Modifier.size(53.dp), shape = CircleShape, color = Surface1) {
                        Box(contentAlignment = Alignment.Center) { Text(c.name.take(2).uppercase(), color = Lime, style = MaterialTheme.typography.titleLarge) }
                    }
                    ScreenHeading(c.name, if (c.role == "buyer") "Покупатель" else "Собственник")
                }
            }
        }
        item {
            AppCard {
                if (c.phone.isNotBlank()) TextButton({ ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + c.phone))) }) { Text("Позвонить: ${c.phone}") }
                if (c.budget > 0) Text("Бюджет: ${c.budget.rub()}")
                if (c.notes.isNotBlank()) Text(c.notes, color = Muted)
            }
        }
        if (c.role == "buyer") item {
            var editCriteria by remember { mutableStateOf(false) }
            AppCard {
                Text("Требования к квартире", style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(c.searchArea, c.desiredKind, c.wishes, when(c.preferredMarket) { "primary" -> "Первичка"; "secondary" -> "Вторичка"; else -> null }, c.desiredRooms.takeIf { it > 0 }?.let { "$it комн." },
                    c.minArea.takeIf { it > 0 }?.let { "от $it м²" }).joinToString(" · ").ifBlank { "Все районы и планировки" }, color = Muted)
                TextButton({ editCriteria = true }) { Text("Изменить подбор") }
            }
            if (editCriteria) {
                var area by remember { mutableStateOf(c.searchArea.orEmpty()) }
                var rooms by remember { mutableStateOf(c.desiredRooms.takeIf { it > 0 }?.toString().orEmpty()) }
                var minimum by remember { mutableStateOf(c.minArea.takeIf { it > 0 }?.toString().orEmpty()) }
                var kind by remember { mutableStateOf(c.desiredKind.orEmpty()) }
                var market by remember { mutableStateOf(c.preferredMarket) }
                var wishes by remember { mutableStateOf(c.wishes.orEmpty()) }
                val size = minimum.replace(',', '.').toDoubleOrNull()
                AlertDialog(onDismissRequest = { editCriteria = false }, title = { Text("Критерии подбора") },
                    text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                        Field(kind, { kind = it }, "Тип жилья: квартира, дом, комната…")
                        val markets = mapOf("Любой" to null, "Первичка" to "primary", "Вторичка" to "secondary")
                        Chips(markets.keys.toList(), markets.entries.firstOrNull { it.value == market }?.key ?: "Любой") { market = markets[it] }
                        Field(wishes, { wishes = it }, "Пожелания через запятую")
                        Field(area, { area = it }, "Район или улица в адресе")
                        Field(rooms, { rooms = it.filter(Char::isDigit).take(2) }, "Количество комнат", keyboard = KeyboardType.Number)
                        Field(minimum, { minimum = it }, "Минимальная площадь, м²", keyboard = KeyboardType.Decimal)
                    } }, confirmButton = { Button(enabled = minimum.isBlank() || (size != null && size.isFinite() && size >= 0), onClick = {
                        vm.saveClient(c.copy(desiredKind = kind.trim().ifBlank { null }, preferredMarket = market, wishes = wishes.trim().ifBlank { null }, searchArea = area.trim().ifBlank { null }, desiredRooms = rooms.toIntOrNull() ?: 0, minArea = size ?: 0.0))
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
        item { Title("История общения") }
        items(history, key = { it.id }) { h -> AppCard { Text(fmtDate(h.at), color = Muted, style = MaterialTheme.typography.bodySmall); Text(h.text) } }
        item { TextButton({ vm.deleteClient(c); back() }) { Text("Удалить клиента", color = MaterialTheme.colorScheme.error) } }
    }
}
