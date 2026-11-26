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
    fun matches(c: Client, props: List<Property>): List<Property> =
        if (c.role != "buyer" || c.budget <= 0 || c.stage in listOf("deal","lost")) emptyList()
        else props.filter { it.status == "active" && it.price in 1..c.budget }.sortedWith(compareByDescending<Property> { it.favorite }.thenBy { it.price }).take(5)
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
    data class Report(val added: Int, val duplicates: Int, val skipped: Int, val changedPrices: Int, val sources: Int, val errors: List<String> = emptyList()) {
        fun summary() = "Добавлено в CRM: $added · уже в базе: $duplicates · пропущено: $skipped · расхождений цен: $changedPrices · фидов: $sources" + if(errors.isEmpty()) "" else "\nОшибки: " + errors.joinToString("; ")
    }
    suspend fun sync(ctx: Context, errors: List<String> = emptyList()): Report = mutex.withLock {
        val hunter = HunterDb.get(ctx)
        val offers = hunter.allOffers()
        val sources = hunter.allSubs().size
        val db = WorkspaceDb.get(ctx)
        val result = db.withTransaction {
            val dao = db.dao()
            val existing = dao.allProperties().toMutableList()
            var added = 0; var duplicates = 0; var skipped = 0; var prices = 0
            for (o in offers.sortedByDescending { it.lastSeen }) {
                val link = AutoRules.urlKey(o.url)
                val addr = AutoRules.addressKey(o.address,o.area,o.rooms,o.floor)
                val marker = AutoRules.sourceKey(o)
                if (!AutoRules.valid(o) || (link == null && addr == null && marker == null)) { skipped++; continue }
                val same = existing.firstOrNull { AutoRules.isDuplicate(o,it) }
                if (same != null) { duplicates++; if(same.price != o.price) prices++; continue }
                val p = Property(title = o.title.ifBlank { o.address.ifBlank { "Объект из импорта" } }, address = o.address, type = o.market,
                    price = o.price, area = o.area, rooms = o.rooms, floor = o.floor,
                    url = o.url.takeIf { link != null }.orEmpty(),
                    description = listOfNotNull("Источник: " + o.source, marker).joinToString("\n"))
                val id = dao.saveProperty(p)
                existing += p.copy(id = id); added++
            }
            Report(added,duplicates,skipped,prices,sources,errors).also { dao.log(RunLog(summary = it.summary())) }
        }
        prefs(ctx).edit().putString("report",result.summary()).putLong("lastRun",System.currentTimeMillis()).apply()
        result
    }
    suspend fun syncIfEnabled(ctx: Context) { if(enabled(ctx)) sync(ctx) }
}
