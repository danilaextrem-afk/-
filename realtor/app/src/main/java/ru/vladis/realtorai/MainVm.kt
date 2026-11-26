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
    fun addInteraction(clientId: Long, text: String) = viewModelScope.launch { dao.saveInteraction(Interaction(clientId = clientId, text = text)) }
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

    fun clearChat() { chat.clear(); error = null }

    fun ask(mode: String, text: String, p: Property?, c: Client?) {
        if (!hasKey) {
            chat.add("user" to text)
            chat.add("assistant" to ru.vladis.realtorai.local.OfflineAssistant.answer(mode, text, p, c, properties.value))
            error = null; return
        }
        var content = text
        p?.let { content += "\n\nОбъект: ${it.title}, ${it.address}, ${it.price} ₽, ${it.area} м², ${it.rooms} комн., этаж ${it.floor}. ${it.description.take(600)}" }
        c?.let { content += "\n\nКлиент: ${it.name}, бюджет ${it.budget} ₽, ${it.notes}" }
        var system = Prompts.BASE + " " + (Prompts.modes.firstOrNull { it.first == mode }?.second ?: "")
        if (mode == "Подбор") system += "\nОбъекты:\n" + properties.value.filter { it.status == "active" }.take(40)
            .joinToString("\n") { "- ${it.title}; ${it.address}; ${it.price} ₽; ${it.area} м²; ${it.rooms}к; ${if (it.type == "primary") "первичка" else "вторичка"}" }
        val hist = chat.toList() + ("user" to content)
        chat.add("user" to text); error = null; busy = true
        viewModelScope.launch {
            runCatching { Groq.chat(store.apiKey, model, system, hist) }
                .onSuccess { chat.add("assistant" to it) }
                .onFailure { error = it.message }
            busy = false
        }
    }

    // ---- Резервная копия (API-ключ в копию не попадает) ----
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        Gson().toJson(Backup(dao.allProperties(), dao.allClients(), dao.allInteractions(), dao.allTasks()))
    }
    suspend fun importJson(s: String) = withContext(Dispatchers.IO) {
        val b = Gson().fromJson(s, Backup::class.java)
        val previous = db.withTransaction {
            val previousTasks = dao.allTasks()
            dao.clearInteractions()
            dao.clearTasks()
            dao.clearClients()
            dao.clearProperties()
        b.properties.forEach { dao.saveProperty(it) }; b.clients.forEach { dao.saveClient(it) }
        b.interactions.forEach { dao.saveInteraction(it) }
            b.tasks.forEach { dao.saveTask(it) }
            previousTasks
        }
        previous.forEach { Reminder.cancel(ctx, it.id) }
        b.tasks.filterNot { it.done }.forEach { Reminder.schedule(ctx, it.id, it.title, it.dueAt) }
    }
}
