package ru.vladis.realtorai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
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

@Composable fun Properties(vm: MainVm, openAnalytics: () -> Unit, openOwner: (Long) -> Unit, openMap: () -> Unit, openListing: (Long) -> Unit) {
    val props by vm.properties.collectAsState()
    val listings by vm.listings.collectAsState()
    val covers = remember(listings) { listings.filter { it.propertyId != null && it.cover.isNotBlank() }.associate { it.propertyId to it.cover } }
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
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            ScreenHeading("Недвижимость", "${props.size} объектов в рабочей базе") {
                FilledTonalIconButton({ edit = Property(title = "") }) { Icon(Icons.Default.Add, "Объект") }
            }
            Spacer(Modifier.height(8.dp))
            Field(q, { q = it }, "Поиск по адресу или названию")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton({ showImport = true }) { Text("Импорт") }
                OutlinedButton(openMap) { Icon(Icons.Default.Place, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Карта") }
                TextButton(openAnalytics) { Text("Аналитика") }
            }
            Chips(listOf("Все", "Первичка", "Вторичка", "Избранное"), type) { type = it }
            Chips(listOf("Любые", "1", "2", "3", "4+"), rooms) { rooms = it }
        }
        if (list.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Place, null, Modifier.size(34.dp), tint = Lime)
                Text(if (props.isEmpty()) "Добавьте первый объект" else "Совпадений нет", style = MaterialTheme.typography.titleLarge)
                Text(if (props.isEmpty()) "Импортируйте объявление по ссылке или из текста." else "Измените поиск или фильтры.", color = Muted)
                if (props.isEmpty()) FilledTonalButton({ showImport = true }) { Text("Импортировать объявление") }
            }
        }
        items(list, key = { it.id }) { p ->
            Surface(onClick = { edit = p }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Surface1) {
                Column {
                    val cover = covers[p.id]
                    if (!cover.isNullOrBlank()) {
                        ru.vladis.realtorai.publish.Thumb(cover, Modifier.fillMaxWidth().height(188.dp))
                    }
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (p.price > 0) p.price.rub() else "Цена уточняется", style = MaterialTheme.typography.headlineSmall)
                                Text(p.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            FilledTonalIconButton({ vm.toggleFav(p) }) {
                                Icon(if (p.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    if (p.favorite) "Убрать из избранного" else "В избранное", tint = Lime)
                            }
                        }
                        if (p.address.isNotBlank()) Text(p.address, color = Muted, style = MaterialTheme.typography.bodySmall)
                        val facts = listOfNotNull(p.area.takeIf { it > 0 }?.let { "$it м²" },
                            p.rooms.takeIf { it > 0 }?.let { "$it комн." }, p.floor.takeIf { it > 0 }?.let { "$it этаж" },
                            p.pricePerSqm().takeIf { it > 0 }?.let { it.rub() + "/м²" })
                        if (facts.isNotEmpty()) Text(facts.joinToString("  ·  "), color = Muted)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ваш доход: ${vm.earning(p).rub()}", Modifier.weight(1f), color = Lime, style = MaterialTheme.typography.labelMedium)
                            TextButton({ openOwner(p.id) }) { Text("Сопровождение") }
                            p.sourceOfferId?.let { id -> TextButton({ openListing(id) }) { Text("Объявление") } }
                        }
                    }
                }
            }
        }
    }
    edit?.let { PropertyDialog(vm, it, { edit = null }) }
    if (showImport) ImportDialog(vm, { showImport = false; edit = it }, { showImport = false })
}

@Composable fun PropertyDialog(vm: MainVm, init: Property, onDismiss: () -> Unit) {
    val clients by vm.clients.collectAsState()
    var title by remember { mutableStateOf(init.title) }
    var kind by remember { mutableStateOf(init.housingKind.orEmpty()) }
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
                Field(kind, { kind = it }, "Тип жилья")
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
                vm.saveProperty(init.copy(housingKind = kind.trim().ifBlank { null }, title = title.trim(), address = address.trim(), price = price.toLongOrNull() ?: 0,
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
