package ru.vladis.realtorai

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.withTransaction
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.vladis.realtorai.ai.*
import ru.vladis.realtorai.data.*

data class Backup(val properties: List<Property>, val clients: List<Client>,
                  val interactions: List<Interaction>, val tasks: List<TaskItem>)

class MainVm(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val db = WorkspaceDb.get(app)
    private val dao = db.dao()
    private val store = Store(app)
    private fun <T> Flow<T>.state(init: T) = stateIn(viewModelScope, SharingStarted.Eagerly, init)

    val listings = db.publications().listings().state(emptyList())
    val properties = dao.properties().state(emptyList())
    val searchableProperties = combine(dao.properties(), db.hunter().offers(), ru.vladis.realtorai.hunter.GeoPreferences.observe(ctx)) { properties, offers, scope ->
        ru.vladis.realtorai.hunter.GeoScope.searchableProperties(properties, offers, scope)
    }.state(emptyList())

    val clients = dao.clients().state(emptyList())
    val tasks = dao.tasks().state(emptyList())
    fun interactions(id: Long) = dao.interactions(id)

    var hasKey by mutableStateOf(store.apiKey.isNotBlank()); private set
    var model by mutableStateOf(store.model); private set
    var cloudReady by mutableStateOf(CloudAi.enabled(ctx)); private set
    fun configureCloud(key: String?, enabled: Boolean) {
        if (key != null) store.cloudKey = key.trim()
        store.cloudEnabled = enabled && store.cloudKey.isNotBlank()
        cloudReady = CloudAi.enabled(ctx)
    }
    fun removeCloud() = configureCloud("", false)
    suspend fun testCloud(): String = CloudAi.answer(ctx, "Ответь одним словом по-русски.", "Скажи: готово")
    var agencyPct by mutableStateOf(store.agencyPct); private set
    var sharePct by mutableStateOf(store.sharePct); private set

    fun saveSettings(key: String?, model: String, agency: Double, share: Double) {
        if (key != null) { store.apiKey = key.trim(); hasKey = key.isNotBlank() }
        store.model = model.trim().ifBlank { "llama-3.3-70b-versatile" }; this.model = store.model
        store.agencyPct = agency; agencyPct = agency
        store.sharePct = share; sharePct = share
    }
    fun clearKey() { store.apiKey = ""; hasKey = false }

    /** Доход агента = цена × комиссия агентства % × доля агента % */
    fun earning(p: Property): Double = p.price * agencyPct / 100 * sharePct / 100

    fun saveProperty(p: Property) = viewModelScope.launch { dao.saveProperty(p) }
    fun deleteProperty(p: Property) = viewModelScope.launch {
        val tasks = db.withTransaction {
            dao.deleteProperty(p)
            db.owners().clearEvents(p.id)
            db.owners().delete(p.id)
            dao.allTasks().filter { it.kind.startsWith("owner/${p.id}/") }.onEach { dao.deleteTask(it) }
        }
        tasks.forEach { Reminder.cancel(ctx, it.id) }
    }
    fun toggleFav(p: Property) = saveProperty(p.copy(favorite = !p.favorite))
    var leadImportStatus by mutableStateOf<String?>(null); private set
    fun importLeads(text: String) = viewModelScope.launch {
        leadImportStatus = runCatching {
            db.withTransaction {
                val result = ru.vladis.realtorai.local.SalesTools.importLeads(text, dao.allClients().map { it.phone })
                result.rows.forEach { dao.saveClient(Client(name = it.name, phone = it.phone, budget = it.budget, role = if (it.owner) "owner" else "buyer")) }
                "Добавлено: ${result.rows.size} · дубли: ${result.duplicates} · ошибки: ${result.rejected}"
            }
        }.getOrElse { "Импорт не выполнен: ${it.message}" }
    }
    fun saveClient(c: Client) = viewModelScope.launch { dao.saveClient(c) }
    fun deleteClient(c: Client) = viewModelScope.launch { dao.deleteInteractions(c.id); dao.deleteClient(c) }
    fun addInteraction(clientId: Long, text: String) = viewModelScope.launch {
        val whenDue = ru.vladis.realtorai.local.MeetingPlan.parse(text, System.currentTimeMillis(), java.time.ZoneId.systemDefault())
        val reminder = db.withTransaction {
            dao.saveInteraction(Interaction(clientId = clientId, text = text))
            if (whenDue == null || !ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(ctx)) return@withTransaction null
            val client = dao.allClients().firstOrNull { it.id == clientId } ?: return@withTransaction null
            val title = "${text.substringBefore(' ').trimEnd(':')} · ${client.name}"
            if (dao.allTasks().any { it.title == title && it.dueAt == whenDue && it.kind == "meeting" }) return@withTransaction null
            val meeting = TaskItem(title = title, dueAt = whenDue, kind = "meeting")
            meeting.copy(id = dao.saveTask(meeting))
        }
        reminder?.let { Reminder.schedule(ctx, it.id, it.title, it.dueAt) }
    }
    fun saveTask(t: TaskItem) = viewModelScope.launch {
        val id = dao.saveTask(t)
        if (t.done) Reminder.cancel(ctx, id) else Reminder.schedule(ctx, id, t.title, t.dueAt)
    }
    fun deleteTask(t: TaskItem) = viewModelScope.launch { dao.deleteTask(t); Reminder.cancel(ctx, t.id) }

    suspend fun fetchPage(url: String) = Importer.fetch(url)
    suspend fun parseListing(text: String, url: String): Property {
        val base = Importer.parse(text, url)
        return if (hasKey) runCatching { Importer.aiRefine(store.apiKey, model, text, base) }.getOrDefault(base) else base
    }

    // ---- ИИ-ассистент ----
    val chat = mutableStateListOf<Pair<String, String>>()
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)

    var deviceReady by mutableStateOf(DeviceModel.installed(ctx)); private set
    var modelProgress by mutableStateOf<Int?>(null); private set
    var activeModel by mutableStateOf(DeviceModel.selected(ctx)); private set
    var downloading by mutableStateOf<ModelSpec?>(null); private set
    private var downloadJob: kotlinx.coroutines.Job? = null
    fun hasModel(spec: ModelSpec) = DeviceModel.installed(ctx, spec)
    fun partialModel(spec: ModelSpec) = DeviceModel.partial(ctx, spec)
    fun chooseModel(spec: ModelSpec) {
        DeviceModel.select(ctx, spec)
        activeModel = spec
        deviceReady = true
    }
    var modelRevision by mutableIntStateOf(0); private set
    var modelSpeed by mutableStateOf(""); private set
    var benchmark by mutableStateOf(""); private set
    fun recommendedModel() = DeviceModel.recommended(ctx)
    fun removeModel(spec: ModelSpec) = viewModelScope.launch {
        try {
            DeviceModel.remove(ctx, spec)
            activeModel = DeviceModel.selected(ctx)
            deviceReady = DeviceModel.installed(ctx)
            modelRevision++
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { error = e.localizedMessage }
    }
    fun benchmarkModel() {
        if (busy || !deviceReady) return
        busy = true
        benchmark = "Запуск проверки установленных моделей"
        viewModelScope.launch {
            val reports = mutableListOf<String>()
            var fastest: Pair<String, Long>? = null
            val installed = models.filter { DeviceModel.installed(ctx, it) }
                .sortedBy { if (it.file.startsWith("Qwen3.5")) 0L else it.size }
            try {
                for (spec in installed) {
                    benchmark = (reports + "Проверяется ${spec.label}…").joinToString("\n")
                    val start = android.os.SystemClock.elapsedRealtime()
                    val line = try {
                        val output = DeviceModel.answer(ctx, "Отвечай строго в запрошенном формате.", listOf("user" to
                            """Нужно найти объекты не дороже семи миллионов рублей. Для поиска используй FIND. Назови по-русски помещение для приготовления еды. Верни только JSON с ключами tool, args (объект с числовым budget) и room."""), spec)
                        val ms = android.os.SystemClock.elapsedRealtime() - start
                        val json = runCatching { com.google.gson.JsonParser.parseString(
                            output.substringAfterLast("</think>").trim().removePrefix("```json").removeSuffix("```").trim()).asJsonObject }.getOrNull()
                        val passed = runCatching { json?.get("tool")?.asString == "FIND" &&
                            json?.getAsJsonObject("args")?.get("budget")?.asLong == 7000000L &&
                            json?.get("room")?.asString.equals("кухня", true) }.getOrDefault(false)
                        if (passed && ms < (fastest?.second ?: Long.MAX_VALUE)) fastest = spec.label to ms
                        "${spec.label} · ${ms / 1000.0} с · " + if (passed) "формат и русский ответ верны" else "тест формата или русского ответа не пройден"
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (e: Exception) { "${spec.label} · ${e.localizedMessage.orEmpty()}" }
                    reports += line
                }
            } finally {
                benchmark = (reports + (fastest?.let { "Быстрее всех из прошедших тест: ${it.first}" }
                    ?: "Ни одна модель не прошла контрольное задание")).joinToString("\n")
                busy = false
            }
        }
    }
    fun importModel(spec: ModelSpec, uri: android.net.Uri) {
        if (downloading != null || busy) return
        downloading = spec
        modelProgress = 0
        modelSpeed = ""
        error = null
        downloadJob = viewModelScope.launch {
            try {
                DeviceModel.importVerified(ctx, uri, spec) { withContext(Dispatchers.Main) { modelProgress = it } }
                chooseModel(spec)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = e.localizedMessage }
            finally { downloading = null; modelProgress = null }
        }
    }
    fun pauseModel() { downloadJob?.cancel() }
    fun installModel(spec: ModelSpec) {
        if (downloading != null) return
        downloading = spec; modelProgress = 0; error = null; modelSpeed = ""
        val initial = partialModel(spec)
        val started = android.os.SystemClock.elapsedRealtime()
        downloadJob = viewModelScope.launch {
            try {
                DeviceModel.download(ctx, spec) { progress ->
                    val seconds = (android.os.SystemClock.elapsedRealtime() - started).coerceAtLeast(1) / 1000.0
                    val speed = (spec.size * progress / 100 - initial).coerceAtLeast(0) / 1_000_000.0 / seconds
                    withContext(Dispatchers.Main) {
                        modelProgress = progress
                        modelSpeed = String.format(java.util.Locale.getDefault(), "%.1f МБ/с", speed)
                    }
                }
                chooseModel(spec)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                error = e.localizedMessage
            } finally { downloading = null; modelProgress = null }
        }
    }

    private var generationJob: kotlinx.coroutines.Job? = null
    var streamingAnswer by mutableStateOf(""); private set
    val cancellableGeneration get() = busy && activeModel.file.endsWith(".gguf")
    fun stopGeneration() { generationJob?.cancel() }
    fun clearChat() { if (!busy) { chat.clear(); error = null } }

    fun canSearch(text: String) = ru.vladis.realtorai.agent.ListingSearch.accepts(text)

    fun ask(mode: String, text: String, p: Property?, c: Client?) {
        if (busy) return
        if (canSearch(text)) {
            chat.add("user" to text)
            error = null
            busy = true
            generationJob = viewModelScope.launch {
                try {
                    val results = withContext(Dispatchers.IO) { ru.vladis.realtorai.agent.ListingSearch.run(ctx, db, text, refresh = true) }
                    chat.add("assistant" to results)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { error = e.localizedMessage }
                finally { busy = false }
            }
            return
        }
        if (!deviceReady && !cloudReady) { error = "Подключите облачный ИИ или скачайте локальную модель"; return }
        val system = Prompts.BASE + " " + (Prompts.modes.firstOrNull { it.first == mode }?.second ?: "")
        val content = buildString {
            c?.let { appendLine("Клиент: ${it.name.take(50)}; бюджет ${it.budget} ₽; ${listOfNotNull(it.searchArea, it.desiredKind, it.wishes).joinToString().take(100)}. ${it.notes.take(80)}") }
            p?.let { appendLine("Объект: ${it.title.take(65)}, ${it.address.take(80)}, ${it.price} ₽, ${it.area} м², ${it.rooms} комн. ${it.description.take(90)}") }
            if (mode == "Подбор") {
                val candidates = c?.let { ru.vladis.realtorai.autopilot.AutoRules.matches(it, searchableProperties.value) }
                    ?: searchableProperties.value.filter { it.status == "active" }
                candidates.take(3).forEach { appendLine("${it.title.take(35)} · ${it.address.take(50)} · ${it.price} ₽ · ${it.area} м²") }
                if (candidates.isEmpty()) appendLine("Подходящих объектов в базе нет.")
            }
            append(text.take(400))
        }
        val hist = chat.toList() + ("user" to content)
        if (busy) return
        chat.add("user" to text); error = null; busy = true
        generationJob = viewModelScope.launch {
            try {
                val answer = if (cloudReady) CloudAi.answer(ctx, system, text) else DeviceModel.answer(ctx, system, hist) { partial ->
                    viewModelScope.launch { if (generationJob?.isActive == true) streamingAnswer = partial }
                }
                chat.add("assistant" to answer)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = e.localizedMessage }
            finally { busy = false; streamingAnswer = "" }
        }
    }

    suspend fun exportBackup(uri: android.net.Uri, password: CharArray) = withContext(Dispatchers.IO) {
        checkNotNull(ctx.contentResolver.openOutputStream(uri, "wt")).use { raw ->
            BackupEncryption.output(raw, password).use { encrypted -> WorkspaceBackup.write(ctx, db, encrypted) }
        }
    }

    suspend fun isEncryptedBackup(uri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        checkNotNull(ctx.contentResolver.openInputStream(uri)).buffered().use(BackupEncryption::detected)
    }

    suspend fun importBackup(uri: android.net.Uri, password: CharArray? = null) = withContext(Dispatchers.IO) {
        val previous = checkNotNull(ctx.contentResolver.openInputStream(uri)).buffered().use { input ->
            if (!BackupEncryption.detected(input)) WorkspaceBackup.read(ctx, db, input)
            else {
                val temporary = java.io.File.createTempFile("restore-", ".zip", ctx.cacheDir)
                try {
                    temporary.outputStream().use { BackupEncryption.decrypt(input, it, requireNotNull(password)) }
                    temporary.inputStream().use { WorkspaceBackup.read(ctx, db, it) }
                } finally { temporary.delete() }
            }
        }
        previous.forEach { Reminder.cancel(ctx, it.id) }
        dao.allTasks().filterNot { it.done }.forEach { Reminder.schedule(ctx, it.id, it.title, it.dueAt) }
    }
}
