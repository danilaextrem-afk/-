package ru.vladis.realtorai.office

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.ui.*

private data class ShareChoice(val localId: Long, val name: String, val data: JSONObject)

@Composable
internal fun OfficeScreen(vm: MainVm, close: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val settings = remember(context) { OfficeSettings(context) }
    val scope = rememberCoroutineScope()
    val properties by vm.properties.collectAsState()
    val listings by vm.listings.collectAsState()
    val clients by vm.clients.collectAsState()
    var session by remember { mutableStateOf(settings.session()) }
    var url by remember { mutableStateOf(session?.origin.orEmpty()) }
    var token by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<Employee?>(null) }
    var section by remember { mutableStateOf(OfficeSection.PROPERTIES) }
    var rows by remember { mutableStateOf(emptyList<OfficeRow>()) }
    var page by remember { mutableStateOf("") }
    var employees by remember { mutableStateOf(emptyList<Employee>()) }
    var assignTarget by remember { mutableStateOf<OfficeRow?>(null) }
    var createDeal by remember { mutableStateOf(false) }
    var dealTransition by remember { mutableStateOf<Pair<OfficeRow, DealStage>?>(null) }
    var chooseShare by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<ShareChoice?>(null) }
    var advertisingPolicy by remember { mutableStateOf<Pair<OfficeFeed, JSONObject>?>(null) }
    var ownerPortal by remember { mutableStateOf<Pair<OfficeRow, JSONObject>?>(null) }
    var ownerLink by remember { mutableStateOf<String?>(null) }
    var advertisingOffer by remember { mutableStateOf<Triple<OfficeFeed, OfficeRow, JSONObject>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }

    fun operation(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        status = null
        problem = null
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: java.io.IOException) { problem = "Не удалось связаться с HTTPS-сервером офиса." }
            catch (e: Exception) { problem = e.message ?: "Действие не выполнено." }
            finally { busy = false }
        }
    }

    suspend fun refresh(client: OfficeClient) {
        val result = client.records(section)
        rows = result.rows
        page = result.next
    }

    LaunchedEffect(Unit) {
        session?.let { stored ->
            operation {
                val client = OfficeClient(stored)
                current = client.me()
                refresh(client)
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(close) { Text("Назад") }
            ScreenHeading("Общая база офиса", "Объекты, покупатели, сделки и ответственные сотрудники")
        }
        if (current == null) item {
            Text("Подключите рабочую учётную запись", style = MaterialTheme.typography.titleLarge)
            Field(url, { url = it.take(400) }, "Адрес сервера")
            Field(token, { token = it.trim().take(100) }, "Токен сотрудника", password = true)
            Button({
                val account = OfficeSession(url.trim(), token)
                operation {
                    val client = OfficeClient(account)
                    val employee = client.me()
                    refresh(client)
                    settings.save(account)
                    session = account
                    current = employee
                    token = ""
                }
            }, enabled = !busy && url.isNotBlank() && token.isNotBlank()) { Text("Подключиться") }
        } else {
            item {
                val profile = current!!
                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                Text(when (profile.role) { "admin" -> "Администратор"; "manager" -> "Руководитель"; else -> "Риелтор" }, color = Muted)
                if (profile.canAssign) {
                    Text("Корпоративное размещение", style = MaterialTheme.typography.titleSmall)
                    Row {
                        OfficeFeed.entries.forEach { feed ->
                            TextButton({
                                operation {
                                    val client = OfficeClient(requireNotNull(session))
                                    advertisingPolicy = feed to client.advertisingPolicy(feed)
                                }
                            }, enabled = !busy) { Text(feed.label) }
                        }
                    }
                }
                Chips(OfficeSection.entries.map { it.label }, section.label) { label ->
                    if (!busy && label != section.label) {
                        section = OfficeSection.entries.first { it.label == label }
                        rows = emptyList()
                        page = ""
                        operation { refresh(OfficeClient(requireNotNull(session))) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({
                        if (section == OfficeSection.DEALS) createDeal = true else chooseShare = true
                    }, enabled = !busy) {
                        Text(if (section == OfficeSection.DEALS) "Открыть сделку" else "Добавить из CRM")
                    }
                    TextButton({ operation { refresh(OfficeClient(requireNotNull(session))) } }, enabled = !busy) { Text("Обновить") }
                }
            }
            if (rows.isEmpty() && !busy && problem == null) item { Text("Нет доступных записей", color = Muted) }
            items(rows, key = { it.id }) { row ->
                if (section == OfficeSection.DEALS) {
                    OfficeDealCard(row, !busy && (current?.canAssign == true || current?.id == row.assignedTo)) {
                        dealTransition = row to it
                    }
                    return@items
                }
                val field = if (section == OfficeSection.PROPERTIES) "title" else "name"
                val amount = row.data.optLong(if (section == OfficeSection.PROPERTIES) "price" else "budget")
                AppCard {
                    Text(row.data.optString(field), style = MaterialTheme.typography.titleMedium)
                    Text(row.data.optString(if (section == OfficeSection.PROPERTIES) "address" else "district"), color = Muted)
                    if (amount > 0) Text(amount.rub(), color = Lime)
                    Text("Ответственный: ${row.assignedTo}", color = Muted)
                    if (section == OfficeSection.PROPERTIES &&
                        (current?.canAssign == true || current?.id == row.assignedTo)) {
                        Row {
                            OfficeFeed.entries.forEach { feed ->
                                TextButton({
                                    operation {
                                        val client = OfficeClient(requireNotNull(session))
                                        advertisingOffer = Triple(feed, row, client.feedOffer(feed, row.id))
                                    }
                                }, enabled = !busy) { Text(feed.label) }
                            }
                        }
                        TextButton({
                            operation { ownerPortal = row to OfficeClient(requireNotNull(session)).ownerReport(row.id) }
                        }, enabled = !busy) { Text("Отчёт собственнику") }
                    }
                    if (current?.canAssign == true) TextButton({
                        operation {
                            employees = OfficeClient(requireNotNull(session)).employees()
                            assignTarget = row
                        }
                    }, enabled = !busy) { Text("Назначить сотрудника") }
                }
            }
            if (page.isNotBlank()) item {
                TextButton({
                    operation {
                        val next = OfficeClient(requireNotNull(session)).records(section, page)
                        rows = (rows + next.rows).distinctBy { it.id }
                        page = next.next
                    }
                }, enabled = !busy) { Text("Показать ещё") }
            }
        }
        problem?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        status?.let { item { Text(it, color = Lime) } }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (session != null) item {
            TextButton({
                settings.disconnect()
                session = null
                current = null
                rows = emptyList()
                employees = emptyList()
                page = ""
            }, enabled = !busy) { Text("Отключить офис") }
        }
    }

    ownerPortal?.let { (row, snapshot) ->
        OfficeOwnerReport(row, snapshot, busy, close = { ownerPortal = null },
            save = { revision, views, inquiries, note ->
                operation {
                    val client = OfficeClient(requireNotNull(session))
                    ownerPortal = row to client.saveOwnerReport(row.id, revision, views, inquiries, note)
                    status = "Отчёт собственнику обновлён"
                }
            }, issue = {
                operation {
                    val client = OfficeClient(requireNotNull(session))
                    ownerLink = client.issueOwnerLink(row.id)
                    ownerPortal = null
                }
            }, revoke = {
                operation {
                    val client = OfficeClient(requireNotNull(session))
                    client.revokeOwnerLink(row.id)
                    ownerPortal = row to client.ownerReport(row.id)
                    status = "Доступ собственника закрыт"
                }
            }, problem = problem)
    }
    ownerLink?.let { link -> OfficeOwnerLink(link) { ownerLink = null } }
    advertisingPolicy?.let { (feed, policy) ->
        OfficeAdvertisingPolicy(feed, policy, current?.role == "admin", { advertisingPolicy = null }) { enabled, limit ->
            advertisingPolicy = null
            operation {
                OfficeClient(requireNotNull(session)).saveAdvertisingPolicy(feed, enabled, limit)
                status = "Правила корпоративного размещения сохранены"
            }
        }
    }
    advertisingOffer?.let { (feed, row, offer) ->
        val local = properties.firstOrNull { settings.recordId(it.id) == row.id }
        val draft = local?.let { property -> listings.firstOrNull { it.propertyId == property.id } }
        OfficeAdvertisingOffer(feed, row, offer, local, draft, { advertisingOffer = null }, { materials ->
            advertisingOffer = null
            operation {
                OfficeClient(requireNotNull(session)).approveFeedOffer(feed, row, offer.getInt("revision"), materials)
                status = "Объект доступен в фиде. Ожидается получение площадкой."
            }
        }, {
            advertisingOffer = null
            operation {
                OfficeClient(requireNotNull(session)).withdrawFeedOffer(feed, row.id, offer.getInt("revision"))
                status = "Объект исключён из фида"
            }
        })
    }
    if (createDeal) {
        val client = remember(session) { OfficeClient(requireNotNull(session)) }
        OfficeDealPicker(client, dismiss = { createDeal = false }) { property, buyer ->
            createDeal = false
            operation {
                client.share(OfficeSection.DEALS, java.util.UUID.randomUUID().toString(),
                    officeDealData(property.id, buyer.id, DealStage.QUALIFICATION), null)
                refresh(client)
                status = "Сделка открыта"
            }
        }
    }
    dealTransition?.let { (row, stage) ->
        OfficeDealTransition(row, stage, dismiss = { dealTransition = null }) { amount, commission ->
            dealTransition = null
            operation {
                val client = OfficeClient(requireNotNull(session))
                client.share(OfficeSection.DEALS, row.id, officeDealData(
                    row.data.getString("property_id"), row.data.getString("buyer_id"), stage, amount, commission
                ), row.revision)
                refresh(client)
                status = "Этап сделки обновлён"
            }
        }
    }
    if (chooseShare) {
        val choices = when (section) {
            OfficeSection.PROPERTIES -> properties.filter { it.status == "active" && it.title.isNotBlank() }
                .map { ShareChoice(it.id, it.title, it.officeData()) }
            OfficeSection.BUYERS -> clients.filter { it.role == "buyer" && !it.doNotContact && it.name.isNotBlank() }
                .map { ShareChoice(it.id, it.name, it.officeData()) }
            OfficeSection.DEALS -> emptyList()
        }
        AlertDialog(onDismissRequest = { chooseShare = false }, title = { Text("Выбрать запись из CRM") }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                if (choices.isEmpty()) Text("В CRM пока нет доступных записей")
                choices.forEach { option ->
                    TextButton({ shareTarget = option; chooseShare = false }) { Text(option.name) }
                }
            }
        }, confirmButton = { TextButton({ chooseShare = false }) { Text("Отмена") } })
    }
    shareTarget?.let { choice ->
        AlertDialog(onDismissRequest = { shareTarget = null }, title = { Text("Передать в общую базу?") },
            text = { Text(choice.name + if (section == OfficeSection.BUYERS)
                "\nИмя и параметры заявки увидят ответственный и руководство." else
                "\nХарактеристики увидят сотрудники офиса.") },
            confirmButton = {
                Button({
                    val recordId = settings.recordId(choice.localId)
                    shareTarget = null
                    operation {
                        val client = OfficeClient(requireNotNull(session))
                        val revision = rows.firstOrNull { it.id == recordId }?.revision
                        client.share(section, recordId, choice.data, revision)
                        refresh(client)
                        status = "Запись обновлена в общей базе"
                    }
                }) { Text("Передать") }
            },
            dismissButton = { TextButton({ shareTarget = null }) { Text("Отмена") } })
    }
    assignTarget?.let { target ->
        AlertDialog(onDismissRequest = { assignTarget = null }, title = { Text("Ответственный сотрудник") }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                employees.forEach { employee ->
                    TextButton({
                        assignTarget = null
                        operation {
                            val client = OfficeClient(requireNotNull(session))
                            client.assign(section, target, employee)
                            refresh(client)
                            status = "Ответственный обновлён"
                        }
                    }, enabled = employee.id != target.assignedTo) { Text(employee.name) }
                }
            }
        }, confirmButton = { TextButton({ assignTarget = null }) { Text("Закрыть") } })
    }
}
