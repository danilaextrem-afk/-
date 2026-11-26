package ru.vladis.realtorai.ui

import androidx.compose.foundation.layout.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.vladis.realtorai.local.SalesTools
import ru.vladis.realtorai.local.ProposalPdf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.*
import ru.vladis.realtorai.local.OfflineLogic

@Composable private fun WorkspaceHeader(title: String, subtitle: String, back: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }
        Column { Text(subtitle, color = Lime, style = MaterialTheme.typography.labelMedium); Title(title) }
    }
}

@Composable fun SalesWorkspace(vm: MainVm, back: () -> Unit) {
    var price by remember { mutableStateOf("") }; var down by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }; var years by remember { mutableStateOf("20") }
    var objection by remember { mutableStateOf("") }; var response by remember { mutableStateOf("") }
    val tasks by vm.tasks.collectAsState(); val clients by vm.clients.collectAsState()
    val clip = LocalClipboardManager.current
    val props by vm.properties.collectAsState()
    val ctx = LocalContext.current; val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf<Long?>(null) }; var propertyId by remember { mutableStateOf<Long?>(null) }
    var cMenu by remember { mutableStateOf(false) }; var pMenu by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }; var suggestions by remember { mutableStateOf(emptyList<String>()) }
    var proposal by remember { mutableStateOf("") }; var pdfStatus by remember { mutableStateOf("") }
    var pendingPdf by remember { mutableStateOf("") }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val content = pendingPdf
        if (uri != null && content.isNotBlank()) scope.launch {
            pdfStatus = runCatching { withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { ProposalPdf.write(content, it) } ?: error("Файл недоступен") } }.fold({ "PDF сохранён" }, { "Ошибка: ${it.message}" })
        }
    }
    val p = price.toLongOrNull(); val d = down.toLongOrNull() ?: 0
    val r = rate.replace(',', '.').toDoubleOrNull(); val y = years.toIntOrNull()
    val payment = if (p != null && d in 0..p && r != null && y != null && y in 1..50) OfflineLogic.mortgage(p-d,r,y*12) else null
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { WorkspaceHeader("Продажи", "Sales Assistant", back) }
        item { AppCard {
            Text("Ипотека и комиссия", style = MaterialTheme.typography.titleLarge)
            Text("Аннуитетный расчёт без страховок и сборов", color = Muted)
            Field(price, { price = it.filter(Char::isDigit) }, "Стоимость, ₽", keyboard = KeyboardType.Number)
            Field(down, { down = it.filter(Char::isDigit) }, "Первоначальный взнос, ₽", keyboard = KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(rate, { rate = it }, "Ставка, %", Modifier.weight(1f), KeyboardType.Decimal)
                Field(years, { years = it.filter(Char::isDigit) }, "Лет", Modifier.weight(1f), KeyboardType.Number)
            }
            Spacer(Modifier.height(10.dp)); Text(payment?.let { "${it.rub()} / мес." } ?: "Введите параметры", color = Lime, style = MaterialTheme.typography.headlineSmall)
            p?.let { Text("Комиссия агента: ${(it * vm.agencyPct / 100 * vm.sharePct / 100).rub()}", color = Muted) }
        } }
        item { AppCard {
            Text("Работа с возражениями", style = MaterialTheme.typography.titleLarge)
            Text("Локальные сценарии, не генеративный ИИ", color = Muted)
            Field(objection, { objection = it }, "Что сказал клиент?", single = false)
            Button({ response = OfflineLogic.objection(objection) }, enabled = objection.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Подготовить ответ") }
            if (response.isNotBlank()) { Text(response); TextButton({ clip.setText(AnnotatedString(response)) }) { Text("Копировать черновик") } }
        } }
        item { AppCard {
            Text("Переписка и предложение", style = MaterialTheme.typography.titleLarge)
            Text("Выберите клиента и объект из общей CRM", color = Muted)
            Box { OutlinedButton({ cMenu = true }, Modifier.fillMaxWidth()) { Text(clients.find { it.id == clientId }?.name ?: "Выбрать клиента") }
                DropdownMenu(cMenu, { cMenu = false }) { clients.forEach { c -> DropdownMenuItem({ Text(c.name) }, { clientId = c.id; cMenu = false }) } } }
            Box { OutlinedButton({ pMenu = true }, Modifier.fillMaxWidth()) { Text(props.find { it.id == propertyId }?.title ?: "Выбрать объект") }
                DropdownMenu(pMenu, { pMenu = false }) { props.filter { it.status == "active" }.forEach { p -> DropdownMenuItem({ Text(p.title) }, { propertyId = p.id; price = p.price.takeIf { it > 0 }?.toString().orEmpty(); pMenu = false }) } } }
            VoiceInput({ words -> transcript = listOf(transcript, words).filter(String::isNotBlank).joinToString(" "); suggestions = emptyList() }, { suggestions = listOf(it) })
            Field(transcript, { transcript = it; suggestions = emptyList() }, "Текст переписки / разговора", single = false)
            Button({ suggestions = SalesTools.conversation(transcript) }, enabled = transcript.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Найти следующие шаги") }
            suggestions.forEach { Text("• $it", color = Muted) }
            OutlinedButton({ clientId?.let { vm.addInteraction(it, transcript.trim()) } }, enabled = clientId != null && transcript.isNotBlank()) { Text("Сохранить переписку в CRM") }
            Button({ val obj = props.find { it.id == propertyId }; if (obj != null) proposal = SalesTools.proposal(clients.find { it.id == clientId }?.name.orEmpty(), obj.title, obj.address, obj.price, obj.area, obj.rooms) }, enabled = props.any { it.id == propertyId && it.status == "active" }, modifier = Modifier.fillMaxWidth()) { Text("Подготовить предложение") }
            if (proposal.isNotBlank()) {
                Field(proposal, { proposal = it }, "Черновик", single = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ clip.setText(AnnotatedString(proposal)) }) { Text("Копировать") }
                    Button({ pendingPdf = proposal; pdf.launch("realtor-proposal.pdf") }) { Text("PDF") }
                }
            }
            if (pdfStatus.isNotBlank()) Text(pdfStatus, color = Muted)
        } }
        item { AppCard {
            Text("Конверсия CRM", style = MaterialTheme.typography.titleLarge)
            val won = clients.count { it.stage == "deal" }
            Text(if (clients.isEmpty()) "Нет данных" else "${won * 100 / clients.size}% · $won из ${clients.size} клиентов", color = Lime, style = MaterialTheme.typography.headlineSmall)
            Text("Доля клиентов со статусом «Сделка»", color = Muted)
        } }
        item { Title("Следующие действия") }
        items(tasks.filterNot { it.done }.take(12)) { task -> AppCard { Text(task.title); Text(fmtDate(task.dueAt), color = Muted); TextButton({ vm.saveTask(task.copy(done = true)) }) { Text("Выполнено") } } }
        if (tasks.none { !it.done }) item { Text("Открытых задач нет", color = Muted) }
    }
}

@Composable fun LeadWorkspace(vm: MainVm, back: () -> Unit, openClient: (Long) -> Unit) {
    val clients by vm.clients.collectAsState(); val props by vm.properties.collectAsState()
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var budget by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Покупатель") }; var message by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("Все") }
    var batch by remember { mutableStateOf("") }; var preview by remember { mutableStateOf(false) }
    val batchResult = remember(batch, clients) { SalesTools.importLeads(batch, clients.map { it.phone }) }
    val active = clients.filter { it.stage !in listOf("deal","lost") }
    val forecast = active.filter { it.role == "buyer" && it.budget > 0 }.sumOf { it.budget * vm.agencyPct / 100 * vm.sharePct / 100 * OfflineLogic.probability(it.stage) }
    val list = active.filter { selected == "Все" || it.role == if (selected == "Покупатели") "buyer" else "owner" }
        .sortedByDescending { OfflineLogic.priority(it.stage, it.budget, it.phone.isNotBlank()) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { WorkspaceHeader("Лиды", "Lead Center", back) }
        item { AppCard {
            Text("Условный прогноз комиссии", color = Muted)
            Text(forecast.rub(), color = Lime, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("На основе бюджетов покупателей и фиксированных весов этапов, не гарантированный доход", color = Muted)
        } }
        item { AppCard {
            Text("Добавить лид", style = MaterialTheme.typography.titleLarge)
            Chips(listOf("Покупатель", "Собственник"), role) { role = it }
            Field(name, { name = it }, "Имя")
            Field(phone, { phone = it }, "Телефон", keyboard = KeyboardType.Phone)
            Field(budget, { budget = it.filter(Char::isDigit) }, "Бюджет / цена, ₽", keyboard = KeyboardType.Number)
            Button({
                val key = OfflineLogic.phoneKey(phone)
                if (key.isNotEmpty() && clients.any { OfflineLogic.phoneKey(it.phone) == key }) message = "Контакт уже существует в CRM"
                else { vm.saveClient(Client(name = name.trim(), phone = phone.trim(), budget = budget.toLongOrNull() ?: 0, role = if (role == "Покупатель") "buyer" else "owner")); name = ""; phone = ""; budget = ""; message = "Лид добавлен в CRM" }
            }, Modifier.fillMaxWidth(), enabled = name.isNotBlank()) { Text("Добавить в CRM") }
            if (message.isNotBlank()) Text(message, color = Muted)
        } }
        item { AppCard {
            Text("Импорт контактов", style = MaterialTheme.typography.titleLarge)
            Text("Одна строка: имя;телефон;бюджет;покупатель или собственник", color = Muted)
            Field(batch, { batch = it; preview = false }, "Вставьте строки", single = false)
            OutlinedButton({ preview = true }, enabled = batch.isNotBlank()) { Text("Проверить импорт") }
            if (preview) {
                Text("К добавлению: ${batchResult.rows.size} · дубли: ${batchResult.duplicates} · ошибки: ${batchResult.rejected}", color = Lime)
                batchResult.rows.take(5).forEach { Text(it.name + " · " + it.budget.rub(), color = Muted) }
                Button({ vm.importLeads(batch); batch = ""; preview = false }, enabled = batchResult.rows.isNotEmpty()) { Text("Добавить в CRM") }
            }
            vm.leadImportStatus?.let { Text(it, color = Muted) }
        } }
        item { Title("Воронка") }
        item { AppCard {
            listOf("new" to "Новые", "contact" to "Контакт", "showing" to "Показ", "deposit" to "Задаток", "deal" to "Сделка", "lost" to "Потеряны").forEach { (id,label) ->
                val count = clients.count { it.stage == id }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(count.toString(), color = Lime) }
                LinearProgressIndicator(progress = { if (clients.isEmpty()) 0f else count.toFloat()/clients.size }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            }
        } }
        item { Title("Приоритетные контакты"); Chips(listOf("Все", "Покупатели", "Собственники"), selected) { selected = it } }
        items(list, key = { it.id }) { c -> AppCard(onClick = { openClient(c.id) }) {
            val score = OfflineLogic.priority(c.stage, c.budget, c.phone.isNotBlank())
            Text(c.name, style = MaterialTheme.typography.titleMedium); Text("Приоритет $score/100 • ${c.budget.rub()}", color = Lime)
            if (c.role == "buyer") Text("По бюджету подходит: ${props.count { it.status == "active" && it.price > 0 && c.budget > 0 && it.price <= c.budget }}", color = Muted)
            TextButton({ vm.saveTask(TaskItem(title = "Связаться: ${c.name}", dueAt = System.currentTimeMillis() + 3600000)) }) { Text("Напомнить через час") }
        } }
        if (list.isEmpty()) item { Text("Нет активных контактов", color = Muted) }
    }
}
