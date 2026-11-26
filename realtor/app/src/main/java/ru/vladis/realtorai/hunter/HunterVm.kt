package ru.vladis.realtorai.hunter

import android.app.Application
import android.location.Geocoder
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.vladis.realtorai.automation.IntegrationRegistry
import ru.vladis.realtorai.data.Client
import java.util.Locale

class HunterVm(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val dao = HunterDb.get(app)
    private val prefs = app.getSharedPreferences("hunter", 0)
    val deals: StateFlow<List<Deal>> = combine(dao.offers(), dao.points()) { o, p -> Deal.build(o, p) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val filters = dao.filters().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val subs = dao.subs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun pointsOf(id: Long) = dao.pointsOf(id)

    var busy by mutableStateOf(false); private set
    var log by mutableStateOf<String?>(null)
    var monitoring by mutableStateOf(prefs.getBoolean("monitoring", false)); private set
    var picks by mutableStateOf<List<Deal>>(emptyList()); private set
    var aiText by mutableStateOf<String?>(null)
    val negotiation = mutableStateMapOf<Long, String>()

    private fun err(e: Throwable) = e.message ?: e.javaClass.simpleName
    private inline fun work(crossinline b: suspend () -> Unit) = viewModelScope.launch { busy = true; log = null; b(); busy = false }

    fun changeMonitoring(on: Boolean) { monitoring = on; prefs.edit().putBoolean("monitoring", on).apply(); HuntWorker.schedule(ctx, on || ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(ctx)) }

    fun importFile(uri: Uri) = work {
        runCatching {
            val t = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.trimStart('\uFEFF')
            val rows = if (t.trimStart().let { it.startsWith("[") || it.startsWith("{") }) HunterImport.fromJson(t, "file") else HunterImport.fromCsv(t, "file")
            HunterImport.ingest(dao, rows).also { ru.vladis.realtorai.autopilot.AutopilotEngine.syncIfEnabled(ctx) }
        }.onSuccess { log = it.describe() }.onFailure { log = "Импорт: ${err(it)}" }
    }
    fun importText(text: String, url: String) = work {
        runCatching { HunterImport.ingest(dao, listOf(HunterImport.fromText(text, url))).also { ru.vladis.realtorai.autopilot.AutopilotEngine.syncIfEnabled(ctx) } }
            .onSuccess { log = it.describe() }.onFailure { log = "Импорт: ${err(it)}" }
    }
    fun addSub(name: String, url: String, format: String) = viewModelScope.launch { dao.saveSub(Subscription(name = name.trim(), url = url.trim(), format = format)) }
    fun deleteSub(s: Subscription) = viewModelScope.launch { dao.deleteSub(s) }
    fun refreshNow() = work {
        val s = HuntRun.refreshAll(ctx); if (ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(ctx)) ru.vladis.realtorai.autopilot.AutopilotEngine.sync(ctx, s.errors); val n = HuntRun.notifyDeals(ctx, s)
        log = if (subs.value.isEmpty()) "Нет подписок на фиды — данные берутся только из импорта." else
            "Новых: ${s.newIds.size}, снижений цены: ${s.dropIds.size}, подходящих под фильтры: $n" + (if (s.errors.isNotEmpty()) ". Ошибки: ${s.errors.joinToString()}" else "")
    }
    /** Подключённые официальные источники (IntegrationRegistry.listingSources). Сейчас пусто — реальных интеграций нет. */
    fun searchSources(query: String) = work {
        val srcs = IntegrationRegistry.listingSources
        if (srcs.isEmpty()) { log = "Нет подключённых источников. Используйте импорт файла, фида или текста."; return@work }
        var added = 0
        srcs.forEach { s -> runCatching { s.search(query) }.onSuccess { ps ->
            added += HunterImport.ingest(dao, ps.map { p -> Offer(source = s.name, externalId = p.url, url = p.url, title = p.title, address = p.address,
                market = p.type, price = p.price, area = p.area, rooms = p.rooms, floor = p.floor, fingerprint = "") }).newIds.size
        }.onFailure { log = "${s.name}: ${err(it)}" } }
        if (log == null) log = "Из источников добавлено: $added"
    }

    fun toggleFav(o: Offer) = viewModelScope.launch { dao.saveOffer(o.copy(favorite = !o.favorite)) }
    fun deleteOffer(id: Long) = viewModelScope.launch { dao.deletePoints(id); dao.deleteOffer(id) }
    fun saveFilter(f: SavedFilter) = viewModelScope.launch { dao.saveFilter(f) }
    fun deleteFilter(f: SavedFilter) = viewModelScope.launch { dao.deleteFilter(f) }

    /** Координаты через системный Geocoder (может быть недоступен). Точность не гарантируется — проверяйте на карте. */
    fun geocodeMissing() = work {
        if (!Geocoder.isPresent()) { log = "Геокодер недоступен на устройстве. Добавьте колонки lat/lon в импорт."; return@work }
        val g = Geocoder(ctx, Locale("ru")); var ok = 0
        withContext(Dispatchers.IO) {
            deals.value.map { it.o }.filter { it.lat == null && it.address.isNotBlank() }.take(50).forEach { o ->
                runCatching {
                    @Suppress("DEPRECATION") g.getFromLocationName(o.address, 1)?.firstOrNull()?.let { dao.saveOffer(o.copy(lat = it.latitude, lon = it.longitude)); ok++ }
                }
            }
        }
        log = "Координаты определены: $ok (до 50 за запуск; проверьте точность на карте)"
    }

    /** Локальный предотбор — без Groq. */
    fun matchLocal(c: Client) { picks = Facts.prefilter(c.budget, deals.value); aiText = null }
    fun matchAi(c: Client) = work {
        runCatching { HunterAi.match(ctx, c, picks) }.onSuccess { aiText = it }.onFailure { log = "ИИ: ${err(it)}" }
    }
    fun negotiate(d: Deal, history: List<PricePoint>) = work {
        runCatching { HunterAi.negotiate(ctx, Facts.negotiation(d, history, System.currentTimeMillis())) }
            .onSuccess { negotiation[d.o.id] = it }.onFailure { log = "ИИ: ${err(it)}" }
    }
}
