package ru.vladis.realtorai.autopilot

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.vladis.realtorai.data.*
import ru.vladis.realtorai.hunter.*
import java.net.URI
import java.util.Locale

object AutoRules {
    fun urlKey(value: String): String? = runCatching {
        val uri = URI(value.trim())
        val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return@runCatching null
        if (uri.scheme?.lowercase(Locale.ROOT) !in listOf("http", "https")) return@runCatching null
        val path = uri.rawPath.orEmpty().trimEnd('/')
        if (path.isBlank() && uri.rawQuery.isNullOrBlank()) return@runCatching null
        val query = uri.rawQuery.orEmpty().split('&').filter { it.isNotBlank() && !it.substringBefore('=').lowercase(Locale.ROOT).startsWith("utm_") && it.substringBefore('=') !in listOf("fbclid","gclid") }.sorted().joinToString("&")
        host + path + if(query.isBlank()) "" else "?" + query
    }.getOrNull()
    private fun normalize(s: String) = s.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    fun addressKey(address: String, area: Double, rooms: Int, floor: Int): String? =
        if (address.trim().length >= 8 && area.isFinite() && area > 0 && rooms > 0 && floor > 0)
            "${normalize(address)}|${Math.round(area * 10)}|$rooms|$floor" else null
    fun sourceKey(o: Offer): String? = o.externalId.trim().takeIf { it.isNotEmpty() }?.let { "[AUTO-SOURCE:${o.source.trim().replace("\n"," ").take(100)}:${it.replace("\n"," ").take(250)}]" }
    fun valid(o: Offer) = o.active && o.deal == "sale" && o.price > 0 && o.area.isFinite() && o.area > 0
    fun preferenceScore(c: Client, details: String): Int = c.wishes.orEmpty().split(',')
        .map { it.trim().lowercase(Locale.ROOT) }.distinct().count { it.isNotEmpty() && details.contains(it, true) }

    fun matches(c: Client, props: List<Property>): List<Property> =
        if (c.role != "buyer" || c.budget <= 0 || c.stage in listOf("deal","lost")) emptyList()
        else props.filter { it.status == "active" && it.price in 1..c.budget &&
            (c.desiredRooms == 0 || it.rooms == c.desiredRooms) &&
            (c.desiredKind == null || c.desiredKind.equals(it.housingKind, true)) &&
            (c.preferredMarket == null || c.preferredMarket == it.type) && it.area >= c.minArea && (c.searchArea.isNullOrBlank() || (it.address + " " + it.description.lineSequence().firstOrNull { line -> line.startsWith("Район:") }.orEmpty()).contains(c.searchArea.trim(), ignoreCase = true)) }.sortedWith(compareByDescending<Property> { preferenceScore(c, it.title + " " + it.description + " " + it.address) }.thenByDescending { it.favorite }.thenBy { it.price }).take(5)
    fun isDuplicate(o: Offer, p: Property): Boolean {
        val u = urlKey(o.url)
        val a = addressKey(o.address,o.area,o.rooms,o.floor)
        val s = sourceKey(o)
        return (u != null && u == urlKey(p.url)) || (a != null && a == addressKey(p.address,p.area,p.rooms,p.floor)) || (s != null && p.description.lineSequence().any { it == s })
    }
}

object AutopilotEngine {
    private val mutex = Mutex()
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("autopilot", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = prefs(ctx).getBoolean("enabled", false)
    fun setEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean("enabled", enabled).apply()
        val hunter = ctx.getSharedPreferences("hunter",Context.MODE_PRIVATE).getBoolean("monitoring", false)
        HuntWorker.schedule(ctx, enabled || hunter)
    }
    fun report(ctx: Context) = prefs(ctx).getString("report", "Автопилот ещё не запускался.").orEmpty()
    fun lastRun(ctx: Context) = prefs(ctx).getLong("lastRun", 0)
    data class Report(val added: Int, val duplicates: Int, val skipped: Int, val changedPrices: Int, val sources: Int, val errors: List<String> = emptyList(), val planned: Int = 0) {
        fun summary() = "Добавлено в CRM: $added · уже в базе: $duplicates · пропущено: $skipped · расхождений цен: $changedPrices · источников: $sources · задач создано: $planned" + if(errors.isEmpty()) "" else "\nОшибки: " + errors.joinToString("; ")
    }
    suspend fun sync(ctx: Context, errors: List<String> = emptyList()): Report = mutex.withLock {
        val hunter = HunterDb.get(ctx)
        val offers = hunter.allOffers()
        val sources = hunter.allSubs().size + ru.vladis.realtorai.automation.IntegrationRegistry.listingSources.size
        val db = WorkspaceDb.get(ctx)
        val result = db.withTransaction {
            val dao = db.dao()
            val existing = dao.allProperties().toMutableList()
            val owners = dao.allClients().filter { it.role == "owner" }.associateBy {
                ru.vladis.realtorai.local.OfflineLogic.phoneKey(it.phone)
            }.toMutableMap()
            var added = 0; var duplicates = 0; var skipped = 0; var prices = 0
            for (o in offers.sortedByDescending { it.lastSeen }) {
                val link = AutoRules.urlKey(o.url)
                val addr = AutoRules.addressKey(o.address,o.area,o.rooms,o.floor)
                val marker = AutoRules.sourceKey(o)
                if (!AutoRules.valid(o) || (link == null && addr == null && marker == null)) { skipped++; continue }
                val phone = ru.vladis.realtorai.local.OfflineLogic.phoneKey(o.ownerPhone.orEmpty())
                val owner = if (!o.ownerName.isNullOrBlank() && phone.length in 7..15) {
                    owners[phone] ?: Client(name = o.ownerName, phone = o.ownerPhone.orEmpty(), role = "owner",
                        notes = "Источник: ${o.source}").let { c ->
                        c.copy(id = dao.saveClient(c)).also { owners[phone] = it }
                    }
                } else null
                val same = existing.firstOrNull { AutoRules.isDuplicate(o,it) }
                if (same != null) {
                    duplicates++
                    if (same.price != o.price) prices++
                    val tracksSource = marker != null && same.description.lineSequence().any { it == marker }
                    if ((same.ownerId == null && owner != null) || (tracksSource && same.price != o.price)) {
                        val linked = same.copy(ownerId = same.ownerId ?: owner?.id, price = if (tracksSource) o.price else same.price)
                        dao.saveProperty(linked)
                        existing[existing.indexOf(same)] = linked
                    }
                    continue
                }
                val p = Property(housingKind = o.housingKind, ownerId = owner?.id, title = o.title.ifBlank { o.address.ifBlank { "Объект из импорта" } }, address = o.address, type = o.market,
                    price = o.price, area = o.area, rooms = o.rooms, floor = o.floor,
                    url = o.url.takeIf { link != null }.orEmpty(),
                    description = listOfNotNull("Источник: " + o.source, o.district.takeIf { it.isNotBlank() }?.let { "Район: $it" }, marker).joinToString("\n"))
                val id = dao.saveProperty(p)
                if (enabled(ctx)) {
                    val listing = ru.vladis.realtorai.publish.Listing(propertyId = id, address = p.address,
                        price = p.price, area = p.area, rooms = p.rooms, floor = p.floor, floorsTotal = o.floorsTotal,
                        market = p.type, title = p.title.take(50))
                    db.publications().saveListing(listing.copy(description = ru.vladis.realtorai.publish.SmartAi.factsText(listing).trim()))
                }
                existing += p.copy(id = id); added++
            }
            val planned = if (enabled(ctx)) planContacts(dao) else 0
            Report(added,duplicates,skipped,prices,sources,errors,planned).also { dao.log(RunLog(summary = it.summary())) }
        }
        prefs(ctx).edit().putString("report",result.summary()).putLong("lastRun",System.currentTimeMillis()).apply()
        result
    }
    private suspend fun planContacts(dao: AppDao): Int {
        val now = System.currentTimeMillis()
        val latest = dao.allInteractions().groupBy { it.clientId }.mapValues { (_, rows) -> rows.maxBy { it.at } }
        val tasks = dao.allTasks()
        val known = tasks.mapTo(mutableSetOf()) { it.kind }
        val pending = tasks.filterNot { it.done }.mapTo(mutableSetOf()) { it.kind.substringBeforeLast('/') }
        var added = 0
        for (client in dao.allClients()) {
            val activity = latest[client.id]
            val prefix = "followup/${client.id}"
            val key = "$prefix/${client.stage}-${activity?.id ?: 0}"
            if (prefix in pending || key in known || client.stage in listOf("deal", "lost")) continue
            val age = now - (activity?.at ?: client.createdAt)
            if (client.stage != "new" && age < 2 * 86_400_000L) continue
            val action = when (client.stage) {
                "new" -> "Уточнить запрос"
                "showing" -> "Обсудить итоги показа"
                "deposit" -> "Согласовать следующий шаг сделки"
                else -> "Запланировать повторный контакт"
            }
            dao.saveTask(TaskItem(title = "$action · ${client.name}", kind = key, dueAt = now))
            added++
        }
        return added
    }

    suspend fun syncIfEnabled(ctx: Context) { if(enabled(ctx)) sync(ctx) }
}
