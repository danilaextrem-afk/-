package ru.vladis.realtorai.hunter

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object HuntRun {
    private val http = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()
    private val shareHttp = http.newBuilder().followRedirects(false).followSslRedirects(false)
        .callTimeout(4, TimeUnit.SECONDS).build()
    data class Summary(val newIds: List<Long>, val dropIds: List<Long>, val errors: List<String>)

    suspend fun searchCatalog(ctx: Context, dao: HunterDao, query: String): Summary {
        val added = mutableListOf<Long>()
        val drops = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        val scope = GeoPreferences.read(ctx)
        val requests = if (query.isNotBlank()) listOf(query.trim())
            else scope.locations.take(6).ifEmpty { listOf("Москва", "Московская область") }
        for (source in ru.vladis.realtorai.automation.IntegrationRegistry.enabled(ctx)) {
            for (place in requests) {
                try {
                    val result = HunterImport.ingest(dao, source.search(place).filter { GeoScope.region(it) != null })
                    added += result.newIds
                    drops += result.dropIds
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errors += "${source.name} ($place): ${e.message}"
                }
            }
        }
        return Summary(added, drops, errors)
    }

    suspend fun refreshForListing(ctx: Context, original: ListingOrigin.Original) {
        val host = ListingOrigin.webUrl(original.url)?.host.orEmpty()
        val preferred = when {
            host == "avito.ru" || host.endsWith(".avito.ru") -> "avito"
            host == "cian.ru" || host.endsWith(".cian.ru") -> "cian"
            host == "realty.yandex.ru" -> "yrl"
            else -> null
        }
        val now = System.currentTimeMillis()
        val subscriptions = HunterDb.get(ctx).allSubs()
            .filter { ListingOrigin.webUrl(it.url) != null && now - it.lastFetch >= 30_000L }
            .sortedWith(compareByDescending<Subscription> { it.format == preferred }.thenBy { it.lastFetch })
            .take(2)
        if (subscriptions.isNotEmpty()) refreshFeeds(ctx, subscriptions, shareHttp)
    }

    suspend fun refreshAll(ctx: Context, includeCatalog: Boolean = true): Summary {
        val dao = HunterDb.get(ctx)
        val feeds = refreshFeeds(ctx, dao.allSubs(), http)
        if (!includeCatalog) return feeds
        val query = ctx.getSharedPreferences("autopilot", 0).getString("catalogQuery", "").orEmpty()
        val catalog = searchCatalog(ctx, dao, query)
        return Summary((feeds.newIds + catalog.newIds).distinct(),
            (feeds.dropIds + catalog.dropIds).distinct(), feeds.errors + catalog.errors)
    }

    private suspend fun refreshFeeds(ctx: Context, subscriptions: List<Subscription>, client: OkHttpClient): Summary {
        val dao = HunterDb.get(ctx)
        val newIds = mutableListOf<Long>()
        val drops = mutableListOf<Long>()
        val errs = mutableListOf<String>()
        for (s in subscriptions) {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    client.newCall(Request.Builder().url(s.url).header("User-Agent", "RealtorAI/1.0").build()).execute().use { r ->
                        if (!r.isSuccessful) error("HTTP ${r.code}")
                        val source = checkNotNull(r.body).source()
                        require(!source.request(5_000_001L)) { "Фид превышает 5 МБ" }
                        source.readUtf8() }
                }
                val source = "feed:${s.name}"
                val rows = when(s.format) {
                    "json" -> HunterImport.fromJson(text, source)
                    "yrl" -> RealtyFeed.parse(text, source)
                    "avito", "cian" -> PartnerFeed.parse(text, source, s.format)
                    else -> HunterImport.fromCsv(text, source)
                }
                if (s.format in setOf("yrl", "avito", "cian")) ru.vladis.realtorai.data.WorkspaceDb.get(ctx).withTransaction {
                    val now = System.currentTimeMillis()
                    val result = HunterImport.ingest(dao, rows, now)
                    if (rows.isNotEmpty()) dao.deactivateOlder(source, now)
                    result
                } else HunterImport.ingest(dao, rows)
            }.onSuccess { r -> newIds += r.newIds; drops += r.dropIds; dao.saveSub(s.copy(lastFetch = System.currentTimeMillis(), lastStatus = "OK: " + r.describe())) }
             .onFailure { e -> if (e is kotlinx.coroutines.CancellationException) throw e; errs += "${s.name}: ${e.message}"; dao.saveSub(s.copy(lastFetch = System.currentTimeMillis(), lastStatus = "Ошибка: ${e.message}")) }
        }
        return Summary(newIds.distinct(), drops.distinct(), errs)
    }

    @SuppressLint("MissingPermission")
    suspend fun notifyDeals(ctx: Context, s: Summary): Int {
        if (s.newIds.isEmpty() && s.dropIds.isEmpty()) return 0
        val dao = HunterDb.get(ctx); val all = dao.allOffers().filter { GeoScope.eligible(it) }; val cl = Dedup.clusters(all)
        val ids = (s.newIds + s.dropIds).toSet()
        val hits = Alerts.select(all.filter { it.id in ids }, all, cl, dao.allFilters())
        if (hits.isEmpty()) return 0
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("deals", "Выгодные предложения", NotificationManager.IMPORTANCE_DEFAULT))
        val text = hits.take(3).joinToString("\n") { (o, d) -> "${o.title.ifBlank { o.address }.take(40)} — ${o.price} ₽" + (d?.let { " (${(it * 100).toInt()}% ниже оценки)" } ?: "") }
        val pi = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let { PendingIntent.getActivity(ctx, 0, it, PendingIntent.FLAG_IMMUTABLE) }
        val n = NotificationCompat.Builder(ctx, "deals").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Deal Hunter: подходящих — ${hits.size}").setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(pi).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(ctx).notify(7001, n) }
        return hits.size
    }
}

class HuntWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val leadError = try {
            if (ru.vladis.realtorai.automation.LeadInbox.connected(applicationContext)) {
                val result = ru.vladis.realtorai.automation.LeadInbox.refresh(applicationContext)
                ru.vladis.realtorai.data.WorkspaceDb.get(applicationContext).dao().log(ru.vladis.realtorai.data.RunLog(summary = result))
            }
            emptyList()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { listOf("Канал заявок: ${e.message}") }
        val s = HuntRun.refreshAll(applicationContext);
 if (ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(applicationContext)) ru.vladis.realtorai.autopilot.AutopilotEngine.sync(applicationContext, s.errors + leadError); HuntRun.notifyDeals(applicationContext, s); return Result.success()
    }
    companion object {
        fun schedule(ctx: Context, on: Boolean) {
            val wm = WorkManager.getInstance(ctx)
            if (!on) { wm.cancelUniqueWork("hunter"); return }
            wm.enqueueUniquePeriodicWork("hunter", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HuntWorker>(6, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        }
    }
}
