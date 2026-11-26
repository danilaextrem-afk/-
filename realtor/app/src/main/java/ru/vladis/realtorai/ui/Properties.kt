package ru.vladis.realtorai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.Property
import ru.vladis.realtorai.data.pricePerSqm

@Composable fun Properties(vm: MainVm, openAnalytics: () -> Unit) {
    val props by vm.properties.collectAsState()
    var q by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Все") }
    var rooms by remember { mutableStateOf("Любые") }
    var edit by remember { mutableStateOf<Property?>(null) }
    var showImport by remember { mutableStateOf(false) }
    val list = props.filter {
        (q.isBlank() || it.title.contains(q, true) || it.address.contains(q, true)) &&
            when (type) { "Первичка" -> it.type == "primary"; "Вторичка" -> it.type == "secondary"; "Избранное" -> it.favorite; else -> true } &&
            when (rooms) { "Любые" -> true; "4+" -> it.rooms >= 4; else -> it.rooms.toString() == rooms }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Title("Недвижимость") }
        item { Field(q, { q = it }, "Поиск по названию или адресу") }
        item { Chips(listOf("Все", "Первичка", "Вторичка", "Избранное"), type) { type = it } }
        item { Chips(listOf("Любые", "1", "2", "3", "4+"), rooms) { rooms = it } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ edit = Property(title = "") }) { Icon(Icons.Default.Add, null); Text(" Объект") }
                FilledTonalButton({ showImport = true }) { Text("Импорт") }
                TextButton(openAnalytics) { Text("Аналитика") }
            }
        }
        if (list.isEmpty()) item { Text("Объектов нет. Добавьте вручную или импортируйте объявление.", color = Muted) }
        items(list, key = { it.id }) { p ->
            AppCard(onClick = { edit = p }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        if (p.address.isNotBlank()) Text(p.address, color = Muted)
                    }
                    IconButton({ vm.toggleFav(p) }) { Icon(if (p.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (p.favorite) "Убрать из избранного" else "В избранное", tint = Lime) }
                }
                Text(if (p.price > 0) p.price.rub() else "Цена уточняется", color = Lime, style = MaterialTheme.typography.headlineSmall)
                Text("${p.area} м² · ${p.rooms} комн. · эт. ${p.floor} · ${if (p.type == "primary") "первичка" else "вторичка"}" +
                    (if (p.pricePerSqm() > 0) " · ${p.pricePerSqm().rub()}/м²" else ""), color = Muted)
                Text("Ваш доход: ${vm.earning(p).rub()}", color = Muted)
            }
        }
    }
    edit?.let { PropertyDialog(vm, it, { edit = null }) }
    if (showImport) ImportDialog(vm, { showImport = false; edit = it }, { showImport = false })
}

@Composable fun PropertyDialog(vm: MainVm, init: Property, onDismiss: () -> Unit) {
    val clients by vm.clients.collectAsState()
    var title by remember { mutableStateOf(init.title) }
    var address by remember { mutableStateOf(init.address) }
    var price by remember { mutableStateOf(if (init.price > 0) init.price.toString() else "") }
    var area by remember { mutableStateOf(if (init.area > 0) init.area.toString() else "") }
    var rooms by remember { mutableStateOf(if (init.rooms > 0) init.rooms.toString() else "") }
    var floor by remember { mutableStateOf(if (init.floor > 0) init.floor.toString() else "") }
    var url by remember { mutableStateOf(init.url) }
    var desc by remember { mutableStateOf(init.description) }
    var type by remember { mutableStateOf(if (init.type == "primary") "Первичка" else "Вторичка") }
    var status by remember { mutableStateOf(mapOf("active" to "В работе", "deal" to "Сделка", "archived" to "Архив")[init.status] ?: "В работе") }
    var owner by remember { mutableStateOf(init.ownerId) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (init.id == 0L) "Новый объект" else "Объект") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(title, { title = it }, "Название")
                Field(address, { address = it }, "Адрес")
                Field(price, { price = it.filter(Char::isDigit) }, "Цена, ₽", keyboard = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(area, { area = it }, "м²", Modifier.weight(1f), KeyboardType.Decimal)
                    Field(rooms, { rooms = it.filter(Char::isDigit) }, "Комн.", Modifier.weight(1f), KeyboardType.Number)
                    Field(floor, { floor = it.filter(Char::isDigit) }, "Этаж", Modifier.weight(1f), KeyboardType.Number)
                }
                Chips(listOf("Вторичка", "Первичка"), type) { type = it }
                Chips(listOf("В работе", "Сделка", "Архив"), status) { status = it }
                val owners = clients.filter { it.role == "owner" }
                if (owners.isNotEmpty()) {
                    Text("Собственник", color = Muted)
                    Chips(listOf("—") + owners.map { it.name }, owners.find { it.id == owner }?.name ?: "—") { n -> owner = owners.find { it.name == n }?.id }
                }
                Field(url, { url = it }, "Ссылка")
                Field(desc, { desc = it }, "Описание", single = false)
                if (init.id != 0L) TextButton({ vm.deleteProperty(init); onDismiss() }) { Text("Удалить объект", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = {
                vm.saveProperty(init.copy(title = title.trim(), address = address.trim(), price = price.toLongOrNull() ?: 0,
                    area = area.replace(',', '.').toDoubleOrNull() ?: 0.0, rooms = rooms.toIntOrNull() ?: 0, floor = floor.toIntOrNull() ?: 0,
                    url = url.trim(), description = desc, type = if (type == "Первичка") "primary" else "secondary",
                    status = when (status) { "Сделка" -> "deal"; "Архив" -> "archived"; else -> "active" }, ownerId = owner))
                onDismiss()
            }) { Text("Сохранить") }
        }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
}

@Composable fun ImportDialog(vm: MainVm, onResult: (Property) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Импорт объявления") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(url, { url = it }, "Ссылка (необязательно)")
                OutlinedButton(enabled = url.isNotBlank() && !busy, onClick = {
                    scope.launch {
                        busy = true; msg = null
                        runCatching { vm.fetchPage(url.trim()) }.onSuccess { text = it }
                            .onFailure { msg = "Не удалось загрузить страницу (сайт может блокировать). Вставьте текст объявления вручную." }
                        busy = false
                    }
                }) { Text("Загрузить по ссылке") }
                Field(text, { text = it }, "Текст объявления", Modifier.heightIn(min = 120.dp), single = false)
                Text(if (vm.hasKey) "Данные будут уточнены через ИИ" else "Без API-ключа — простой разбор по шаблонам", color = Muted)
                msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = text.isNotBlank() && !busy, onClick = {
                scope.launch { busy = true; val p = vm.parseListing(text, url.trim()); busy = false; onResult(p) }
            }) { Text(if (busy) "…" else "Разобрать") }
        }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
}
