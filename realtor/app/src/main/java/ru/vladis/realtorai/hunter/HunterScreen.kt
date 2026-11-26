package ru.vladis.realtorai.hunter

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.Store
import ru.vladis.realtorai.ui.*
import java.io.File
import kotlin.math.abs
import kotlin.math.max

private fun pct(d: Double) = "${(d * 100).toInt()}%"

@Composable fun HunterScreen(main: MainVm, back: () -> Unit) {
    val vm: HunterVm = viewModel()
    val deals by vm.deals.collectAsState()
    var tab by remember { mutableStateOf("Лента") }
    var openId by remember { mutableStateOf<Long?>(null) }
    var flt by remember { mutableStateOf(SavedFilter()) }
    var hideDups by remember { mutableStateOf(true) }
    var favOnly by remember { mutableStateOf(false) }
    BackHandler(openId != null) { openId = null }
    val open = deals.find { it.o.id == openId }
    if (open != null) { DealDetail(vm, main, open, deals) { openId = null }; return }
    val sale = deals.filter { it.o.deal == "sale" }
    val shown = sale.filter { Filters.matches(it.o, flt) && (!flt.onlyBelowMarket || it.below) && (!hideDups || it.repr) && (!favOnly || it.o.favorite) }
        .sortedWith(compareByDescending<Deal> { it.discount ?: -1.0 })
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }; Title("AI Deal Hunter") }
        Chips(listOf("Лента", "Карта", "Фильтры", "Данные", "Подбор"), tab) { tab = it }
        if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        vm.log?.let { Text(it, color = Lime, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            "Лента" -> FeedTab(vm, shown, sale.size, flt, { flt = it }, hideDups, { hideDups = it }, favOnly, { favOnly = it }) { openId = it }
            "Карта" -> { Text("На карте: ${shown.count { it.o.lat != null }} из ${shown.size} (нужны координаты). Лаймовый маркер — дешевле рынка.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OfferMap(shown, Modifier.weight(1f).fillMaxWidth()) { openId = it } }
            "Фильтры" -> FiltersTab(vm) { flt = it; tab = "Лента" }
            "Данные" -> DataTab(vm, deals)
            else -> MatchTab(vm, main) { openId = it }
        }
    }
}

@Composable fun DealCard(d: Deal, vm: HunterVm, onOpen: () -> Unit) = AppCard(onClick = onOpen) {
    val o = d.o
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(o.title.ifBlank { o.address.ifBlank { "Объект #${o.id}" } }, fontWeight = FontWeight.SemiBold, maxLines = 2)
            if (o.address.isNotBlank()) Text(o.address, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        IconButton({ vm.toggleFav(o) }) { Icon(if (o.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = Lime) }
    }
    Text("${o.price.rub()} · ${Market.ppsm(o).toLong().rub()}/м²", color = Lime, fontWeight = FontWeight.Bold)
    Text("${o.area} м² · ${o.rooms} комн. · эт. ${o.floor}", color = Muted)
    when {
        d.discount == null -> Text("Оценка рынка: недостаточно аналогов (нужно ≥${Market.MIN_COMPS})", color = Muted, style = MaterialTheme.typography.bodySmall)
        d.below -> Text("Дешевле рынка на ${pct(d.discount)} · по ${d.est!!.comps} аналогам", color = Lime, fontWeight = FontWeight.SemiBold)
        else -> Text("К оценке рынка: ${if (d.discount >= 0) "−" else "+"}${pct(abs(d.discount))} · аналогов ${d.est!!.comps}", color = Muted)
    }
    if (d.dups > 1) Text("Дублей: ${d.dups - 1}", color = Muted, style = MaterialTheme.typography.bodySmall)
    if (d.delta != 0L) Text("Цена ${if (d.delta < 0) "снижена" else "повышена"} на ${abs(d.delta).rub()}", color = if (d.delta < 0) Lime else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable fun FeedTab(vm: HunterVm, shown: List<Deal>, total: Int, f: SavedFilter, onF: (SavedFilter) -> Unit,
                        hide: Boolean, onHide: (Boolean) -> Unit, fav: Boolean, onFav: (Boolean) -> Unit, open: (Long) -> Unit) {
    var price by remember { mutableStateOf("") }; var area by remember { mutableStateOf("") }; var dist by remember { mutableStateOf(f.district) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(price, { price = it.filter(Char::isDigit); onF(f.copy(maxPrice = price.toLongOrNull() ?: 0)) }, "Цена до, ₽", Modifier.weight(1f), KeyboardType.Number)
            Field(area, { area = it; onF(f.copy(minArea = it.replace(',', '.').toDoubleOrNull() ?: 0.0)) }, "Площадь от", Modifier.weight(1f), KeyboardType.Decimal) } }
        item { Chips(listOf("Любые", "1", "2", "3", "4+"), when (f.rooms) { "" -> "Любые"; "4" -> "4+"; else -> f.rooms }) { onF(f.copy(rooms = when (it) { "Любые" -> ""; "4+" -> "4"; else -> it })) } }
        item { Field(dist, { dist = it; onF(f.copy(district = it)) }, "Район / адрес содержит") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(f.onlyBelowMarket, { onF(f.copy(onlyBelowMarket = it)) }); Text("  Только дешевле рынка") }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(hide, onHide); Text("  Скрыть дубли") }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(fav, onFav); Text("  Только избранное") }
        }
        item {
            val name = listOfNotNull(if (f.maxPrice > 0) "до ${f.maxPrice / 1_000_000.0} млн" else null, if (f.rooms.isNotBlank()) "${f.rooms}к" else null, f.district.ifBlank { null }, if (f.onlyBelowMarket) "дешевле рынка" else null).joinToString(", ").ifBlank { "Все" }
            OutlinedButton({ vm.saveFilter(f.copy(id = 0, name = name)) }) { Text("Сохранить фильтр: $name") }
            Text("Показано ${shown.size} из $total (продажа)", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        if (total == 0) item { Text("Данных нет. Вкладка «Данные»: импортируйте CSV/JSON, вставьте объявление или подключите фид. Объявления не генерируются.", color = Muted) }
        items(shown, key = { it.o.id }) { DealCard(it, vm) { open(it.o.id) } }
    }
}

@Composable fun FiltersTab(vm: HunterVm, apply: (SavedFilter) -> Unit) {
    val fs by vm.filters.collectAsState()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (fs.isEmpty()) item { Text("Сохранённых фильтров нет. Задайте параметры в «Ленте» и нажмите «Сохранить фильтр».", color = Muted) }
        items(fs, key = { it.id }) { f ->
            AppCard(onClick = { apply(f) }) {
                Text(f.name, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(f.notify, { vm.saveFilter(f.copy(notify = it)) }); Text("  Уведомлять", Modifier.weight(1f))
                    TextButton({ vm.deleteFilter(f) }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable fun DataTab(vm: HunterVm, deals: List<Deal>) {
    val subs by vm.subs.collectAsState()
    var pasted by remember { mutableStateOf("") }; var url by remember { mutableStateOf("") }
    var sName by remember { mutableStateOf("") }; var sUrl by remember { mutableStateOf("") }; var sFmt by remember { mutableStateOf("csv") }; var q by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) vm.importFile(u) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Text("В базе: ${deals.size} (продажа ${deals.count { it.o.deal == "sale" }}, аренда ${deals.count { it.o.deal == "rent" }}, с координатами ${deals.count { it.o.lat != null }}). Только ваши данные.", color = Muted) }
        item { AppCard {
            Text("Импорт файла CSV/JSON", fontWeight = FontWeight.SemiBold)
            Text("Колонки: цена, площадь, комнат, этаж (или 5/9), этажность, адрес, район, lat, lon, ссылка, id, заголовок, сделка (продажа/аренда), рынок.", color = Muted, style = MaterialTheme.typography.bodySmall)
            OutlinedButton({ pick.launch(arrayOf("text/*", "application/json", "*/*")) }) { Text("Выбрать файл") }
        } }
        item { AppCard {
            Text("Вставить объявление", fontWeight = FontWeight.SemiBold)
            Field(url, { url = it }, "Ссылка (необязательно)"); Spacer(Modifier.height(6.dp))
            Field(pasted, { pasted = it }, "Текст объявления (с строкой «Адрес: …»)", single = false)
            OutlinedButton({ vm.importText(pasted, url.trim()); pasted = "" }, enabled = pasted.isNotBlank()) { Text("Добавить") }
        } }
        item { AppCard {
            Text("Подписка на фид (мониторинг)", fontWeight = FontWeight.SemiBold)
            Text("URL машиночитаемого CSV/JSON, к которому у вас есть законный доступ (например корпоративный/партнёрский фид).", color = Muted, style = MaterialTheme.typography.bodySmall)
            Field(sName, { sName = it }, "Название"); Spacer(Modifier.height(6.dp)); Field(sUrl, { sUrl = it }, "URL фида")
            Chips(listOf("csv", "json"), sFmt) { sFmt = it }
            OutlinedButton({ vm.addSub(sName, sUrl, sFmt); sName = ""; sUrl = "" }, enabled = sName.isNotBlank() && sUrl.startsWith("http")) { Text("Добавить подписку") }
            subs.forEach { s -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("${s.name} (${s.format})"); Text(s.lastStatus.ifBlank { "не обновлялась" }, color = Muted, style = MaterialTheme.typography.bodySmall) }
                TextButton({ vm.deleteSub(s) }) { Text("✕") } } }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(vm.monitoring, { vm.changeMonitoring(it) }); Text("  Мониторинг каждые 6 ч + уведомления") }
            Button({ vm.refreshNow() }, enabled = !vm.busy) { Text("Обновить сейчас") }
        } }
        item { AppCard {
            Text("Подключённые источники", fontWeight = FontWeight.SemiBold)
            Text("Реализации ListingSource (официальные API) регистрируются в IntegrationRegistry; сейчас их нет.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Field(q, { q = it }, "Запрос к источникам"); OutlinedButton({ vm.searchSources(q) }, enabled = q.isNotBlank()) { Text("Искать в источниках") }
        } }
        item { OutlinedButton({ vm.geocodeMissing() }, enabled = !vm.busy) { Text("Определить координаты по адресам (для карты)") } }
    }
}

@Composable fun MatchTab(vm: HunterVm, main: MainVm, open: (Long) -> Unit) {
    val clients by main.clients.collectAsState()
    var sel by remember { mutableStateOf<ru.vladis.realtorai.data.Client?>(null) }; var menu by remember { mutableStateOf(false) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Box {
            OutlinedButton({ menu = true }) { Text(sel?.let { "${it.name} · бюджет ${it.budget.rub()}" } ?: "Выбрать клиента из CRM") }
            DropdownMenu(menu, { menu = false }) { clients.filter { it.role == "buyer" }.forEach { c ->
                DropdownMenuItem({ Text("${c.name} · ${c.budget.rub()}") }, { sel = c; menu = false; vm.matchLocal(c) }) } }
        } }
        item { Text("Шаг 1 — локальный отбор по бюджету и выгодности (бесплатно). Шаг 2 — сводка по отобранным объектам; облачный ИИ только при наличии ключа.", color = Muted, style = MaterialTheme.typography.bodySmall) }
        sel?.let { c -> item { Button({ vm.matchAi(c) }, enabled = vm.picks.isNotEmpty() && !vm.busy) { Text("ИИ-подбор (${vm.picks.size} объектов)") } } }
        vm.aiText?.let { t -> item { AppCard { Text("Рекомендация ИИ", fontWeight = FontWeight.SemiBold); Text(t) } } }
        items(vm.picks, key = { it.o.id }) { DealCard(it, vm) { open(it.o.id) } }
    }
}

@Composable fun PriceChart(p: List<PricePoint>) {
    if (p.size < 2) return
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val mn = p.minOf { it.price }; val rng = max(1L, p.maxOf { it.price } - mn)
        val path = Path(); p.forEachIndexed { i, pt ->
            val x = size.width * i / (p.size - 1); val y = size.height * (1 - (pt.price - mn).toFloat() / rng) * 0.9f + size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        drawPath(path, Lime, style = Stroke(4f))
    }
}

@Composable fun DealDetail(vm: HunterVm, main: MainVm, d: Deal, all: List<Deal>, close: () -> Unit) {
    val ctx = LocalContext.current; val clip = LocalClipboardManager.current
    val o = d.o; val hist by vm.pointsOf(o.id).collectAsState(emptyList())
    val store = remember { Store(ctx) }
    val offers = remember(all) { all.map { it.o } }; val cl = remember(all) { Dedup.clusters(offers) }
    val twins = offers.filter { it.id != o.id && cl[it.id] == cl[o.id] }
    val rent = remember(all, o.id) { Invest.rentEstimate(o, offers, cl) }
    var rentS by remember(o.id) { mutableStateOf(rent?.first?.toString() ?: "") }
    var vac by remember { mutableStateOf("0") }; var costs by remember { mutableStateOf("0") }; var tax by remember { mutableStateOf("0") }
    val inv = Invest.calc(Invest.In(o.price, rentS.toLongOrNull() ?: 0, vac.replace(',', '.').toDoubleOrNull() ?: 0.0, costs.toLongOrNull() ?: 0, tax.replace(',', '.').toDoubleOrNull() ?: 0.0))
    val crmProps by main.properties.collectAsState()
    val exists = crmProps.any { o.url.isNotBlank() && it.url == o.url || o.address.isNotBlank() && it.address.equals(o.address, true) && it.price == o.price && it.area == o.area && it.rooms == o.rooms }
    var saveRequested by remember(o.id) { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(close) { Icon(Icons.Default.ArrowBack, "Назад") }
            Title(o.title.ifBlank { "Объект #${o.id}" }.take(32)); Spacer(Modifier.weight(1f))
            IconButton({ vm.toggleFav(o) }) { Icon(if (o.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = Lime) } } }
        item { AppCard {
            Text(o.price.rub(), color = Lime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("${o.address} ${o.district}".trim(), color = Muted)
            Text("${o.area} м² · ${o.rooms} комн. · эт. ${o.floor}/${o.floorsTotal} · ${Market.ppsm(o).toLong().rub()}/м² · ${if (o.market == "primary") "первичка" else "вторичка"}")
            Text("Источник: ${o.source}", color = Muted, style = MaterialTheme.typography.bodySmall)
            if (o.url.isNotBlank()) TextButton({ runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.url))) } }) { Text("Открыть объявление") }
            Button({ saveRequested = true; main.saveProperty(ru.vladis.realtorai.data.Property(title = o.title.ifBlank { "Квартира" }, address = o.address, type = o.market, price = o.price, area = o.area, rooms = o.rooms, floor = o.floor, url = o.url)) }, enabled = !exists && !saveRequested && o.deal == "sale", modifier = Modifier.fillMaxWidth()) { Text(if (exists) "Уже в CRM" else if (saveRequested) "Сохранение запрошено" else "Добавить в объекты CRM") }
        } }
        item { AppCard {
            Text("Рыночная стоимость", fontWeight = FontWeight.SemiBold)
            val e = d.est
            if (e == null) Text("Недостаточно аналогов: найдено ${Market.comps(o, offers, cl).size}, нужно ≥${Market.MIN_COMPS}. Импортируйте больше данных — оценка не выдумывается.", color = Muted)
            else {
                Text("≈ ${e.value.rub()} (межквартильный диапазон ${e.low.rub()} – ${e.high.rub()})")
                Text("Медиана ${e.medianPpsm.rub()}/м² по ${e.comps} аналогам (комнаты, площадь ±25%, дубли учтены один раз)", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(if (d.below) "Дешевле рынка на ${pct(d.discount!!)}" else "Разница с оценкой: ${(d.discount!! * 100).toInt()}%", color = if (d.below) Lime else Muted, fontWeight = FontWeight.SemiBold)
            }
        } }
        item { AppCard {
            Text("Потенциальная комиссия", fontWeight = FontWeight.SemiBold)
            Text("Агентство (${store.agencyPct}% от цены): ${(o.price * store.agencyPct / 100).rub()}", color = Muted)
            Text("Ваш доход (${store.sharePct}% доли): ${Commission.agent(o.price, store.agencyPct, store.sharePct).rub()}", color = Lime, fontWeight = FontWeight.Bold)
        } }
        item { AppCard {
            Text("История цены", fontWeight = FontWeight.SemiBold)
            if (hist.size < 2) Text("Изменений цены в данных нет (точек: ${hist.size}). История копится при повторных импортах/обновлении фида.", color = Muted)
            PriceChart(hist); hist.forEach { Text("${fmtDate(it.at)} — ${it.price.rub()}", color = Muted, style = MaterialTheme.typography.bodySmall) }
        } }
        if (twins.isNotEmpty()) item { AppCard { Text("Дубли (${twins.size})", fontWeight = FontWeight.SemiBold)
            twins.forEach { Text("• ${it.source}: ${it.price.rub()} · ${it.address}", color = Muted, style = MaterialTheme.typography.bodySmall) } } }
        item { AppCard {
            Text("Инвестиции и аренда", fontWeight = FontWeight.SemiBold)
            Text(rent?.let { "Аренда по ${it.second} аналогам в базе: ≈ ${it.first.rub()}/мес" } ?: "Аналогов аренды в базе <${Market.MIN_COMPS} — введите ставку вручную (или импортируйте объявления об аренде со сделкой «аренда»).", color = Muted, style = MaterialTheme.typography.bodySmall)
            Field(rentS, { rentS = it.filter(Char::isDigit) }, "Аренда, ₽/мес", keyboard = KeyboardType.Number); Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(vac, { vac = it }, "Простой, %", Modifier.weight(1f), KeyboardType.Decimal)
                Field(costs, { costs = it.filter(Char::isDigit) }, "Расходы, ₽/год", Modifier.weight(1f), KeyboardType.Number)
                Field(tax, { tax = it }, "Налог, %", Modifier.weight(1f), KeyboardType.Decimal) }
            if ((rentS.toLongOrNull() ?: 0) > 0) {
                Text("Валовая доходность: ${"%.2f".format(inv.grossYield * 100)}% · чистая: ${"%.2f".format(inv.netYield * 100)}%", color = Lime)
                Text(inv.paybackYears?.let { "Окупаемость: ${"%.1f".format(it)} лет" } ?: "Окупаемость: нет (чистый доход ≤ 0)", color = Muted)
                Text("Простой, расходы и налог — ваши допущения (по умолчанию 0); рост цены не учитывается.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        } }
        item { AppCard {
            Text("Аргументы для переговоров", fontWeight = FontWeight.SemiBold)
            Facts.negotiation(d, hist, System.currentTimeMillis()).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(6.dp))
            Button({ vm.negotiate(d, hist) }, enabled = !vm.busy) { Text("Подготовить аргументы") }
            vm.negotiation[o.id]?.let { t -> Spacer(Modifier.height(6.dp)); Text(t); TextButton({ clip.setText(AnnotatedString(t)) }) { Text("Копировать") } }
            vm.log?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        } }
        item { TextButton({ vm.deleteOffer(o.id); close() }) { Text("Удалить из базы", color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable fun OfferMap(deals: List<Deal>, modifier: Modifier, onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    remember {
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = File(ctx.cacheDir, "osmdroid"); osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
        }
    }
    val map = remember { MapView(ctx).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true)
        controller.setZoom(10.0); controller.setCenter(GeoPoint(55.7558, 37.6173)) } }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) map.onResume() else if (e == Lifecycle.Event.ON_PAUSE) map.onPause() }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs); map.onDetach() }
    }
    AndroidView({ map }, modifier) { m ->
        m.overlays.clear()
        deals.filter { it.o.lat != null && it.o.lon != null }.forEach { d ->
            val mk = Marker(m); mk.position = GeoPoint(d.o.lat!!, d.o.lon!!)
            mk.title = d.o.price.rub(); mk.snippet = d.o.address
            mk.icon = mk.icon.mutate().also { it.setTint((if (d.below) Lime else Muted).toArgb()) }
            mk.setOnMarkerClickListener { _, _ -> onOpen(d.o.id); true }
            m.overlays.add(mk)
        }
        m.invalidate()
    }
}
