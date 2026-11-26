package ru.vladis.realtorai.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.ai.DeviceModel
import ru.vladis.realtorai.data.WorkspaceDb
import ru.vladis.realtorai.ui.*

@Composable
fun OwnerScreen(vm: MainVm, id: Long, openPublish: () -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current.applicationContext
    val db = remember(ctx) { WorkspaceDb.get(ctx) }
    val service = remember(ctx) { OwnerService(ctx) }
    val scope = rememberCoroutineScope()
    val props by vm.properties.collectAsState()
    val property = props.firstOrNull { it.id == id }
    val case by remember(id) { db.owners().observe(id) }.collectAsState(null)
    val history by remember(id) { db.owners().history(id) }.collectAsState(emptyList())
    val offers by remember { db.hunter().offers() }.collectAsState(emptyList())
    val listings by vm.listings.collectAsState()
    val publications by remember { db.publications().publications() }.collectAsState(emptyList())
    val listing = listings.firstOrNull { it.propertyId == id }
    val clip = LocalClipboardManager.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var evidence by remember(case?.step) { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var approval by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    fun action(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.localizedMessage ?: "Операция не выполнена" }
            finally { busy = false }
        }
    }

    val value = case
    val step = value?.let { OwnerStep.valueOf(it.step) }
    val comparable = remember(value?.location, property, offers) {
        if (property == null || value == null) emptyList()
        else OwnerRules.comparisons(property, offers, value.location, System.currentTimeMillis())
    }
    val prices = if (property == null || value == null) emptyList()
        else OwnerRules.estimate(property.area, comparable, PriceStrategy(value.fast, value.normal, value.patient))

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(back) { Text("Назад") }
            ScreenHeading("Сопровождение собственника", property?.title ?: "Карточка объекта")
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { issue -> item { Text(issue, color = MaterialTheme.colorScheme.error) } }
        if (property != null && value == null) item {
            AppCard {
                Text("Первый звонок", style = MaterialTheme.typography.titleLarge)
                Text(OwnerStep.CALL.checklist, color = Muted)
                Button({ action { service.start(id) } }, enabled = !busy) { Text("Начать сопровождение") }
            }
        }
        if (property != null && value != null && step != null) {
            item {
                AppCard {
                    Text(step.title, color = Lime, style = MaterialTheme.typography.headlineSmall)
                    Text("Начато " + fmtDate(value.startedAt), color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OwnerStep.entries.dropLast(1).forEach { stage ->
                            val color = when { stage.ordinal < step.ordinal -> Lime; stage == step -> Lime.copy(alpha = .45f); else -> Surface2 }
                            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp)).background(color))
                        }
                    }
                    Text(step.checklist)
                    if (step != OwnerStep.FINISHED) {
                        Field(evidence, { evidence = it.take(4000) }, "Что подтверждено", single = false)
                        Button({ action { service.advance(id, step, evidence) } }, enabled = !busy && evidence.isNotBlank()) {
                            Text("Подтвердить этап")
                        }
                    }
                    if (step == OwnerStep.AGREEMENT) TextButton({
                        clip.setText(AnnotatedString("Здравствуйте! Предлагаю согласовать условия работы по вашему объекту. Когда вам удобно созвониться или встретиться?"))
                    }) { Text("Скопировать SMS о сотрудничестве") }
                }
            }
            item {
                var show by remember { mutableStateOf(false) }
                AppCard {
                    TextButton({ show = !show }) { Text(if (show) "Свернуть этапы" else "Показать все этапы") }
                    if (show) OwnerStep.entries.forEachIndexed { i, stage ->
                        Text("${i + 1}. ${stage.title}", color = if (i <= step.ordinal) Lime else Muted)
                    }
                }
            }
            item {
                AppCard {
                    Text("Объявление и материалы", style = MaterialTheme.typography.titleLarge)
                    if (listing == null) Text("Свяжите карточку Smart Publish с объектом", color = Muted)
                    else {
                        Text("Фотографий: " + listing.photos.lineSequence().count { it.isNotBlank() })
                        Text(if (listing.description.isBlank()) "Описание ожидается" else "Описание подготовлено")
                        publications.filter { it.listingId == listing.id }.forEach {
                            Text(it.platform + " · " + it.message.ifBlank { it.status }, color = Muted)
                        }
                    }
                    TextButton(openPublish) { Text("Открыть Smart Publish") }
                }
            }
            item { OwnerSettings(value, busy) { update -> action { service.update(update) } } }
            item {
                AppCard {
                    Text("Сравнение цен", style = MaterialTheme.typography.titleLarge)
                    Text("Тот же район, тип жилья, рынок и число комнат. Площадь ±20%. Источники за 30 дней.", color = Muted)
                    if (prices.isEmpty()) Text("Укажите район и тип жилья. Для расчёта нужны минимум три аналога.", color = Muted)
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            comparable.forEach {
                                Text("${it.address} · ${it.area} м² · ${it.price.rub()} · ${(it.price / it.area).rub()}/м²\n${it.source} · ${fmtDate(it.lastSeen)}\n${it.url}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (prices.isNotEmpty()) {
                        Text("Средняя цена за м²: " + comparable.map { it.price / it.area }.average().rub())
                        listOf("Около месяца", "2–6 месяцев", "От полугода").zip(prices).forEach { (label, amount) ->
                            Surface(onClick = { price = amount.toString() }, shape = MaterialTheme.shapes.small,
                                color = if (price == amount.toString()) MaterialTheme.colorScheme.primaryContainer else Surface2) {
                                Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, style = MaterialTheme.typography.bodySmall)
                                    Text(amount.rub(), color = Lime, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Text("Ориентиры по регламенту. Фактический срок зависит от спроса.", color = Muted)
                    }
                    Field(price, { price = it.filter(Char::isDigit).take(13) }, "Новая цена в CRM, ₽")
                    Button({ approval = property.price to price.toLong() },
                        enabled = !busy && price.toLongOrNull()?.let { it in 1..1_000_000_000_000L && it != property.price } == true) {
                        Text("Проверить изменение цены")
                    }
                }
            }
            item {
                AppCard {
                    Text("Отчёт собственнику", style = MaterialTheme.typography.titleLarge)
                    value.activatedAt?.let {
                        val due = OwnerRules.reportDue(value.lastReportAt ?: it, value.reportsPerWeek, java.time.ZoneId.systemDefault())
                        Text("Следующий отчёт " + fmtDate(due), color = Muted)
                    }
                    OutlinedButton({ action {
                        val result = ru.vladis.realtorai.hunter.HuntRun.refreshAll(ctx, includeCatalog = false)
                        draft = service.draftReport(id)
                        if (result.errors.isNotEmpty()) error = result.errors.joinToString()
                    } }, enabled = !busy) { Text("Обновить конкурентов и отчёт") }
                    OutlinedButton({ action { draft = service.draftReport(id) } }, enabled = !busy) { Text("Подготовить отчёт") }
                    if (DeviceModel.installed(ctx)) OutlinedButton({ action {
                        val intro = DeviceModel.answer(ctx, "Напиши краткое вступление к отчёту по продаже объекта. Используй только переданные сведения. Без обещаний, сделок и цифр.",
                            listOf("user" to ("Реклама: " + value.advertising.take(200) + "\nМатериалы: " + value.packaging.take(200))))
                        draft = (intro + "\n\n" + service.draftReport(id)).take(12000)
                    } }, enabled = !busy) { Text("Подготовить с локальной моделью") }
                    if (draft.isNotBlank()) {
                        Field(draft, { draft = it.take(12000) }, "Черновик отчёта", single = false)
                        TextButton({ clip.setText(AnnotatedString(draft)) }) { Text("Скопировать") }
                        Button({ action { service.recordReport(id, draft); draft = "" } }, enabled = !busy) { Text("Сохранить в истории") }
                    }
                }
            }
            item { Title("История работы") }
            items(history, key = { it.id }) { entry ->
                AppCard {
                    Text(fmtDate(entry.at), color = Lime)
                    SelectionContainer { Text(entry.text) }
                }
            }
        }
    }
    approval?.let { (previous, proposed) ->
        AlertDialog(onDismissRequest = { approval = null }, title = { Text("Изменить цену в CRM?") },
            text = { Text("${previous.rub()} → ${proposed.rub()}. Для обновления публикаций откройте Smart Publish.") },
            confirmButton = { Button({
                approval = null
                action { service.changePrice(id, previous, proposed); price = "" }
            }) { Text("Подтвердить цену") } },
            dismissButton = { TextButton({ approval = null }) { Text("Отмена") } })
    }
}

@Composable
private fun OwnerSettings(value: OwnerCase, busy: Boolean, save: (OwnerCase) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var location by remember(value.location) { mutableStateOf(value.location) }
    var frequency by remember(value.reportsPerWeek) { mutableIntStateOf(value.reportsPerWeek) }
    var advertising by remember(value.advertising) { mutableStateOf(value.advertising) }
    var packaging by remember(value.packaging) { mutableStateOf(value.packaging) }
    var tour by remember(value.tour) { mutableStateOf(value.tour) }
    val counts = listOf(value.calls, value.leads, value.views, value.ownerCalls, value.impressions)
    val entries = remember(counts) { mutableStateListOf(*counts.map { it?.toString().orEmpty() }.toTypedArray()) }
    val coefficients = listOf(value.fast, value.normal, value.patient)
    val factors = remember(coefficients) { mutableStateListOf(*coefficients.map { it.toString() }.toTypedArray()) }
    AppCard {
        TextButton({ expanded = !expanded }) { Text(if (expanded) "Свернуть данные" else "Реклама, обращения и настройки") }
        if (expanded) {
            Field(location, { location = it.take(100) }, "Район для сравнения")
            Chips(listOf("Раз в неделю", "Дважды в неделю"), if (frequency == 1) "Раз в неделю" else "Дважды в неделю") {
                frequency = if (it == "Раз в неделю") 1 else 2
            }
            Field(advertising, { advertising = it.take(3000) }, "Подтверждённая реклама", single = false)
            Field(packaging, { packaging = it.take(3000) }, "Фото, описание, планировка", single = false)
            Field(tour, { tour = it.take(512) }, "Ссылка на 3D-тур")
            Text("Пустое поле означает, что данных нет", color = Muted)
            listOf("Звонки", "Обращения", "Показы", "Входящие звонки собственнику", "Просмотры рекламы").forEachIndexed { i, label ->
                Field(entries[i], { entries[i] = it.filter(Char::isDigit).take(9) }, label, keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
            }
            listOf("Коэффициент около месяца", "Коэффициент 2–6 месяцев", "Коэффициент от полугода").forEachIndexed { i, label ->
                Field(factors[i], { factors[i] = it.take(6) }, label, keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal)
            }
            val numbers = factors.map { it.replace(',', '.').toDoubleOrNull() ?: Double.NaN }
            val strategy = PriceStrategy(numbers[0], numbers[1], numbers[2])
            val validTour = tour.isBlank() || runCatching { java.net.URI(tour.trim()).let { it.scheme == "https" && it.host != null } }.getOrDefault(false)
            Button({
                save(value.copy(reportsPerWeek = frequency, location = location.trim(), advertising = advertising.trim(), packaging = packaging.trim(),
                    tour = tour.trim(), calls = entries[0].toIntOrNull(), leads = entries[1].toIntOrNull(), views = entries[2].toIntOrNull(),
                    ownerCalls = entries[3].toIntOrNull(), impressions = entries[4].toIntOrNull(), fast = strategy.fast, normal = strategy.normal, patient = strategy.patient))
            }, enabled = !busy && strategy.valid() && validTour) { Text("Сохранить подтверждённые данные") }
        }
    }
}
