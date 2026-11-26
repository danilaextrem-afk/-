package ru.vladis.realtorai.publish

import android.graphics.BitmapFactory
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.ui.*

private val statusText = mapOf("published" to "опубликовано (подтверждено API)", "exported" to "выгружено в файл — публикация НЕ подтверждена",
    "failed" to "ошибка", "unknown" to "результат не подтверждён — проверьте площадку", "removed" to "снято", "removal_pending" to "ожидает снятия — перевыгрузите фид", "outdated" to "цена не обновлена")

@Composable fun Thumb(path: String, modifier: Modifier) {
    var bmp by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bmp = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }.getOrNull() }
    }
    bmp?.let { Image(it, null, modifier, contentScale = ContentScale.Crop) } ?: Box(modifier.background(Surface2))
}

@Composable fun PublishScreen(main: MainVm, back: () -> Unit) {
    val pvm: PublishVm = viewModel()
    var openId by remember { mutableStateOf<Long?>(null) }
    if (openId == null) ListingList(pvm, main, { openId = it }, back) else ListingEditor(pvm, openId!!) { openId = null }
}

@Composable fun PubLines(pubs: List<Publication>, compact: Boolean) {
    pubs.filter { it.status != "none" }.forEach { p ->
        val pl = Platform.valueOf(p.platform)
        Text("${pl.label}: ${statusText[p.status] ?: p.status}", style = MaterialTheme.typography.bodySmall,
            color = when (p.status) { "published" -> Lime; "failed" -> MaterialTheme.colorScheme.error; else -> Muted })
        if (!compact && p.message.isNotBlank()) Text(p.message, style = MaterialTheme.typography.bodySmall, color = Muted)
        if (!compact && p.url.isNotBlank()) Text(p.url, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable fun ListingList(pvm: PublishVm, main: MainVm, open: (Long) -> Unit, back: () -> Unit) {
    val ls by pvm.listings.collectAsState(); val pubs by pvm.pubs.collectAsState(); val props by main.properties.collectAsState()
    var sel by remember { mutableStateOf(setOf<Long>()) }
    var section by remember { mutableStateOf("Все") }
    val publishedIds = pubs.filter { it.status == "published" }.mapTo(mutableSetOf()) { it.listingId }
    val visible = ls.filter { l -> when (section) {
        "Черновики" -> l.status != "sold" && l.id !in publishedIds
        "Опубликованные" -> l.status != "sold" && l.id in publishedIds
        "Продано" -> l.status == "sold"
        else -> true
    } }
    var menu by remember { mutableStateOf(false) }; var settings by remember { mutableStateOf(false) }; var mass by remember { mutableStateOf(false) }; var fullFeed by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Pair<List<Long>, List<Platform>>?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val p = pending; pending = null; if (uri != null && p != null) pvm.exportTo(uri, p.first, p.second)
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }
            ScreenHeading("Публикации", "Smart Publish · ${ls.size} карточек")
            Spacer(Modifier.height(7.dp))
            Chips(listOf("Все", "Черновики", "Опубликованные", "Продано"), section) {
                section = it
                sel = emptySet()
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ pvm.create(Listing(), open) }) { Text("+ Новая") }
                Box {
                    FilledTonalButton({ menu = true }) { Text("Из объекта") }
                    DropdownMenu(menu, { menu = false }) {
                        props.take(40).forEach { p -> DropdownMenuItem({ Text(p.title.take(40)) }, {
                            menu = false
                            pvm.create(Listing(propertyId = p.id, address = p.address, price = p.price, area = p.area, rooms = p.rooms, floor = p.floor, market = p.type), open)
                        }) }
                    }
                }
                TextButton({ settings = true }) { Text("Площадки") }
            }
        }
        if (sel.isNotEmpty()) item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Выбрано: ${sel.size}", Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.labelMedium)
                    Button({ mass = true }, enabled = !pvm.busy) { Text("Публикация") }
                }
            }
        }
        if (ls.isNotEmpty()) item { TextButton({ fullFeed = true }, enabled = !pvm.busy) { Text("Экспортировать фиды") } }
        if (pvm.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        pvm.log?.let { m -> item { Text(m, color = Lime) } }
        if (visible.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ls.isEmpty()) "Подготовьте первое объявление" else "В этом разделе карточек нет", style = MaterialTheme.typography.titleLarge)
                if (ls.isEmpty()) Text("Создайте карточку или возьмите объект из CRM.", color = Muted)
            }
        }
        items(visible, key = { it.id }) { l ->
            AppCard(onClick = { open(l.id) }) {
                if (l.cover.isNotBlank()) Thumb(l.cover, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(l.id in sel, { sel = if (it) sel + l.id else sel - l.id }, enabled = l.status != "sold")

                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l.title.ifBlank { l.address.ifBlank { "Новая карточка" } }, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(l.price.rub() + if (l.status == "sold") " · ПРОДАНО" else "", color = if (l.status == "sold") Muted else Lime)
                    }
                }
                PubLines(pubs.filter { it.listingId == l.id }, true)
            }
        }
    }
    if (settings) PublishSettingsDialog(pvm) { settings = false }
    if (fullFeed) {
        var chosen by remember { mutableStateOf(setOf<Platform>()) }
        AlertDialog(onDismissRequest = { fullFeed = false }, title = { Text("Полная выгрузка базы приложения") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Все непроданные карточки. Проданные исключаются. Если на площадке есть другие объявления, не заменяйте их этим файлом без сверки.")
                Platform.values().filter { !it.isApi }.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(p in chosen, { chosen = if (it) chosen + p else chosen - p }); Text(p.label)
                } }
            }
        }, confirmButton = { Button(enabled = chosen.isNotEmpty(), onClick = {
            pending = ls.map { it.id } to chosen.toList(); fullFeed = false; export.launch("smart-publish-all.zip")
        }) { Text("Выгрузить") } }, dismissButton = { TextButton({ fullFeed = false }) { Text("Отмена") } })
    }
    if (mass) {
        var chosen by remember { mutableStateOf(setOf<Platform>()) }
        AlertDialog(onDismissRequest = { mass = false }, title = { Text("Массовая публикация") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Platform.values().forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(p in chosen, { chosen = if (it) chosen + p else chosen - p })
                        Column { Text(p.label); Text(if (p.isApi) "реальная отправка через API" else if (p == Platform.VT24) "экспорт карточки (API не подключён)" else "файл фида — вы загружаете его в кабинет", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Объектов: ${sel.size}. Успех публикации засчитывается только по ответу API. По фидам статус «выгружено» ≠ «опубликовано». Выборочный экспорт не заменяет полный фид. Платное продвижение не подключается.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = {
            Button(enabled = chosen.isNotEmpty(), onClick = {
                val ids = sel.toList(); val api = chosen.filter { it.isApi }; val feed = chosen.filter { !it.isApi }
                mass = false
                if (api.isNotEmpty()) pvm.publishApi(ids, api)
                if (feed.isNotEmpty()) { pending = ids to feed; export.launch("smart-publish.zip") }
            }) { Text("Выполнить") }
        }, dismissButton = { TextButton({ mass = false }) { Text("Отмена") } })
    }
}

@Composable fun ListingEditor(pvm: PublishVm, id: Long, close: () -> Unit) {
    val ctx = LocalContext.current
    val ls by pvm.listings.collectAsState(); val pubs by pvm.pubs.collectAsState()
    val cur = ls.find { it.id == id }
    if (cur == null) { LinearProgressIndicator(Modifier.fillMaxWidth()); return }
    var address by remember(id) { mutableStateOf(cur.address) }
    var price by remember(id) { mutableStateOf(if (cur.price > 0) cur.price.toString() else "") }
    var area by remember(id) { mutableStateOf(if (cur.area > 0) cur.area.toString() else "") }
    var rooms by remember(id) { mutableStateOf(if (cur.rooms > 0) cur.rooms.toString() else "") }
    var floor by remember(id) { mutableStateOf(if (cur.floor > 0) cur.floor.toString() else "") }
    var floors by remember(id) { mutableStateOf(if (cur.floorsTotal > 0) cur.floorsTotal.toString() else "") }
    var kitchen by remember(id) { mutableStateOf(if (cur.kitchenArea > 0) cur.kitchenArea.toString() else "") }
    var reno by remember(id) { mutableStateOf(cur.renovation) }
    var extra by remember(id) { mutableStateOf(cur.extra) }
    var phone by remember(id) { mutableStateOf(cur.phone) }
    var market by remember(id) { mutableStateOf(if (cur.market == "primary") "Первичка" else "Вторичка") }
    var title by remember(cur.title) { mutableStateOf(cur.title) }
    var desc by remember(cur.description) { mutableStateOf(cur.description) }
    var preview by remember { mutableStateOf(false) }; var priceDlg by remember { mutableStateOf(false) }
    var soldDlg by remember { mutableStateOf(false) }; var delDlg by remember { mutableStateOf(false) }
    fun draft() = cur.copy(address = address.trim(), price = price.toLongOrNull() ?: 0, area = area.replace(',', '.').toDoubleOrNull() ?: 0.0,
        rooms = rooms.toIntOrNull() ?: 0, floor = floor.toIntOrNull() ?: 0, floorsTotal = floors.toIntOrNull() ?: 0,
        kitchenArea = kitchen.replace(',', '.').toDoubleOrNull() ?: 0.0, renovation = reno.trim(), extra = extra.trim(), phone = phone.trim(),
        market = if (market == "Первичка") "primary" else "secondary", title = title.trim(), description = desc.trim())
    BackHandler { if (!pvm.busy) { pvm.save(draft()); close() } }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { u -> if (u.isNotEmpty()) pvm.addPhotos(draft(), u) }
    val myPubs = pubs.filter { it.listingId == id }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ pvm.save(draft()); close() }, enabled = !pvm.busy) { Icon(Icons.Default.ArrowBack, "Назад") }; Title("Карточка объекта") } }
        if (cur.status == "sold") item { Text("ПРОДАНО — объект исключён из выгрузок", color = Lime, fontWeight = FontWeight.Bold) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cur.photoList(), key = { it }) { p ->
                    Box {
                        Thumb(p, Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).then(if (p == cur.cover) Modifier.border(2.dp, Lime, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { pvm.save(draft().copy(cover = p)) })
                        Text("✕", Modifier.align(Alignment.TopEnd).clickable { pvm.removePhoto(cur, p) }.padding(6.dp), color = Lime)
                    }
                }
            }
            Text("Нажмите на фото — сделать обложкой (изумрудная рамка)", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        item { OutlinedButton({ pick.launch("image/*") }) { Text("Добавить фото (${cur.photoList().size})") } }
        item { Chips(listOf("Вторичка", "Первичка"), market) { market = it } }
        item { Field(address, { address = it }, "Адрес") }
        item { Field(price, { price = it.filter(Char::isDigit) }, "Цена, ₽", keyboard = KeyboardType.Number) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(area, { area = it }, "м²", Modifier.weight(1f), KeyboardType.Decimal)
            Field(kitchen, { kitchen = it }, "Кухня", Modifier.weight(1f), KeyboardType.Decimal)
            Field(rooms, { rooms = it.filter(Char::isDigit) }, "Комн.", Modifier.weight(1f), KeyboardType.Number) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(floor, { floor = it.filter(Char::isDigit) }, "Этаж", Modifier.weight(1f), KeyboardType.Number)
            Field(floors, { floors = it.filter(Char::isDigit) }, "Этажей в доме", Modifier.weight(1f), KeyboardType.Number) } }
        item { Field(reno, { reno = it }, "Ремонт (как есть, со слов)") }
        item { Field(extra, { extra = it }, "Другие подтверждённые факты (метро, дом, условия)", single = false) }
        item { Field(phone, { phone = it }, "Телефон для объявления", keyboard = KeyboardType.Phone) }
        item {
            AppCard {
                Text("ИИ: обложка, заголовок, описание", fontWeight = FontWeight.SemiBold)
                Text("Локальная оценка качества фото и текст по введённым фактам. Проверьте черновик.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button({ pvm.runAi(draft()) }, enabled = !pvm.busy) { Text(if (pvm.busy) "Анализ…" else "Сгенерировать") }
                pvm.log?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        item { Field(title, { title = it }, "Заголовок (${title.length}/50)") }
        item { Field(desc, { desc = it }, "Описание (${desc.length})", single = false) }
        item { val w = SmartAi.check(draft()) + Checks.warnings(draft()); w.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ pvm.save(draft()) }, enabled = !pvm.busy) { Text("Сохранить") }
            FilledTonalButton({ preview = true }) { Text("Предпросмотр") } } }
        item { OutlinedButton({
            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, draft().postText()) }
            ctx.startActivity(Intent.createChooser(share, "Отправить текст объявления"))
        }, modifier = Modifier.fillMaxWidth(), enabled = cur.status != "sold" && title.isNotBlank() && desc.isNotBlank()) { Text("Поделиться текстом без ключей") } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ priceDlg = true }, enabled = cur.status != "sold" && !pvm.busy) { Text("Изменить цену") }
            OutlinedButton({ soldDlg = true }, enabled = !pvm.busy && (cur.status != "sold" || myPubs.any { it.status == "removal_pending" && Platform.valueOf(it.platform).isApi })) { Text(if (cur.status == "sold") "Повторить снятие" else "Продано") } } }
        if (myPubs.any { it.status != "none" }) item { AppCard { Text("Площадки", fontWeight = FontWeight.SemiBold); PubLines(myPubs, false) } }
        item { TextButton({ delDlg = true }) { Text("Удалить карточку", color = MaterialTheme.colorScheme.error) } }
    }
    if (preview) {
        val d = draft()
        AlertDialog(onDismissRequest = { preview = false }, confirmButton = { TextButton({ preview = false }) { Text("Закрыть") } }, title = { Text("Предпросмотр") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (d.cover.isNotBlank()) Thumb(d.cover, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)))
                Text(d.title.ifBlank { "(нет заголовка)" }, fontWeight = FontWeight.Bold)
                Text(d.price.rub(), color = Lime, fontWeight = FontWeight.Bold)
                Text(d.postText().substringAfter("\n\n").substringAfter("\n"), style = MaterialTheme.typography.bodySmall)
                Text("Фото: ${d.photoList().size}. Заголовок: ${d.title.length}/50 (лимит Авито). Telegram: ${d.postText().length} симв.", color = Muted, style = MaterialTheme.typography.bodySmall)
                val miss = Checks.blocking(d); if (miss.isNotEmpty()) Text("Не заполнено: ${miss.joinToString()}", color = MaterialTheme.colorScheme.error)
            }
        })
    }
    if (priceDlg) {
        var np by remember { mutableStateOf(cur.price.toString()) }
        AlertDialog(onDismissRequest = { priceDlg = false }, title = { Text("Новая цена") }, text = {
            Column { Field(np, { np = it.filter(Char::isDigit) }, "Цена, ₽", keyboard = KeyboardType.Number)
                Text("Telegram и ВК обновятся через API. Фид-площадки получат пометку «перевыгрузите фид».", color = Muted, style = MaterialTheme.typography.bodySmall) }
        }, confirmButton = { Button(enabled = (np.toLongOrNull() ?: 0) > 0, onClick = { pvm.save(draft()); pvm.updatePrice(id, np.toLong()); price = np; priceDlg = false }) { Text("Обновить") } },
            dismissButton = { TextButton({ priceDlg = false }) { Text("Отмена") } })
    }
    if (soldDlg) AlertDialog(onDismissRequest = { soldDlg = false }, title = { Text("Объект продан?") },
        text = { Text("Telegram и ВК: публикации будут удалены через API. Авито/Циан/Домклик/Яндекс/VT 24: объект исключается из выгрузок — снятие произойдёт после загрузки нового фида.") },
        confirmButton = { Button({ pvm.save(draft()); pvm.markSold(id); soldDlg = false }) { Text("Снять") } }, dismissButton = { TextButton({ soldDlg = false }) { Text("Отмена") } })
    if (delDlg) AlertDialog(onDismissRequest = { delDlg = false }, title = { Text("Удалить карточку?") },
        text = { Text("Публикации на площадках не снимаются — сначала отметьте объект проданным.") },
        confirmButton = { Button({ pvm.delete(cur); delDlg = false; if (myPubs.none { it.status in setOf("published", "outdated", "removal_pending", "exported", "unknown") }) close() }) { Text("Удалить") } }, dismissButton = { TextButton({ delDlg = false }) { Text("Отмена") } })
}

@Composable fun PublishSettingsDialog(pvm: PublishVm, onDismiss: () -> Unit) {
    val s = pvm.secrets
    var tgT by remember { mutableStateOf(s.get("tg_token")) }; var tgC by remember { mutableStateOf(s.get("tg_chat")) }
    var vkT by remember { mutableStateOf(s.get("vk_token")) }; var vkO by remember { mutableStateOf(s.get("vk_owner")) }
    var vis by remember { mutableStateOf(s.get("vision_model", SmartAi.DEFAULT_VISION)) }
    var base by remember { mutableStateOf(s.get("photo_base")) }; var ph by remember { mutableStateOf(s.get("phone")) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Площадки") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Telegram: бот должен быть администратором канала", color = Muted, style = MaterialTheme.typography.bodySmall)
            Field(tgT, { tgT = it }, "Telegram bot token", password = true); Field(tgC, { tgC = it }, "Канал (@name или -100…)")
            Text("ВКонтакте: токен с правами wall, photos; owner_id группы со знаком «-»", color = Muted, style = MaterialTheme.typography.bodySmall)
            Field(vkT, { vkT = it }, "VK access token", password = true); Field(vkO, { vkO = it }, "VK owner_id")
            Text("Фиды (Авито, Циан, Домклик, Яндекс): фото должны быть доступны по URL", color = Muted, style = MaterialTheme.typography.bodySmall)
            Field(base, { base = it }, "Базовый URL папки с фото"); Field(ph, { ph = it }, "Контактный телефон по умолчанию", keyboard = KeyboardType.Phone)
            Field(vis, { vis = it }, "Vision-модель Groq")
            Text("VT 24: API не подключён — доступен только экспорт.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { Button({
        s.set("tg_token", tgT); s.set("tg_chat", tgC); s.set("vk_token", vkT); s.set("vk_owner", vkO)
        s.set("vision_model", vis); s.set("photo_base", base); s.set("phone", ph); onDismiss()
    }) { Text("Сохранить") } }, dismissButton = { TextButton(onDismiss) { Text("Отмена") } })
}
