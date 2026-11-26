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
    val clients = dao.clients().state(emptyList())
    val tasks = dao.tasks().state(emptyList())
    fun interactions(id: Long) = dao.interactions(id)

    var hasKey by mutableStateOf(store.apiKey.isNotBlank()); private set
    var model by mutableStateOf(store.model); private set
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
    fun deleteProperty(p: Property) = viewModelScope.launch { dao.deleteProperty(p) }
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
    fun installModel() {
        if (modelProgress != null || deviceReady) return
        modelProgress = 0
        error = null
        viewModelScope.launch {
            try {
                DeviceModel.download(ctx) { withContext(Dispatchers.Main) { modelProgress = it } }
                deviceReady = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                error = e.localizedMessage
            } finally { modelProgress = null }
        }
    }

    fun clearChat() { chat.clear(); error = null }

    fun ask(mode: String, text: String, p: Property?, c: Client?) {
        if (!deviceReady) { error = "Сначала скачайте локальную модель"; return }
        var content = text
        p?.let { content += "\n\nОбъект: ${it.title}, ${it.address}, ${it.price} ₽, ${it.area} м², ${it.rooms} комн., этаж ${it.floor}. ${it.description.take(600)}" }
        c?.let { content += "\n\nКлиент: ${it.name}, бюджет ${it.budget} ₽, ${it.notes}" }
        var system = Prompts.BASE + " " + (Prompts.modes.firstOrNull { it.first == mode }?.second ?: "")
        if (mode == "Подбор") system += "\nОбъекты:\n" + (c?.let { ru.vladis.realtorai.autopilot.AutoRules.matches(it, properties.value) } ?: properties.value.filter { it.status == "active" }).take(5)
            .joinToString("\n") { "- ${it.title}; ${it.address}; ${it.price} ₽; ${it.area} м²; ${it.rooms}к; ${if (it.type == "primary") "первичка" else "вторичка"}" }
        val hist = chat.toList() + ("user" to content)
        chat.add("user" to text); error = null; busy = true
        viewModelScope.launch {
            runCatching { DeviceModel.answer(ctx, system, hist) }
                .onSuccess { chat.add("assistant" to it) }
                .onFailure { error = it.message }
            busy = false
        }
    }

    suspend fun exportBackup(uri: android.net.Uri) = withContext(Dispatchers.IO) {
        checkNotNull(ctx.contentResolver.openOutputStream(uri, "wt")).use { WorkspaceBackup.write(ctx, db, it) }
    }

    suspend fun importBackup(uri: android.net.Uri) = withContext(Dispatchers.IO) {
        val previous = checkNotNull(ctx.contentResolver.openInputStream(uri)).use { WorkspaceBackup.read(ctx, db, it) }
        previous.forEach { Reminder.cancel(ctx, it.id) }
        dao.allTasks().filterNot { it.done }.forEach { Reminder.schedule(ctx, it.id, it.title, it.dueAt) }
    }
}
