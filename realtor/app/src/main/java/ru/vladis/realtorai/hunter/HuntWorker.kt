package ru.vladis.realtorai.hunter

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object HuntRun {
    private val http = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()
    data class Summary(val newIds: List<Long>, val dropIds: List<Long>, val errors: List<String>)

    suspend fun searchCatalog(ctx: Context, dao: HunterDao, query: String): Summary {
        val added = mutableListOf<Long>()
        val drops = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        for (source in ru.vladis.realtorai.automation.IntegrationRegistry.enabled(ctx)) {
            try {
                val result = HunterImport.ingest(dao, source.search(query))
                added += result.newIds
                drops += result.dropIds
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                errors += "${source.name}: ${e.message}"
            }
        }
        return Summary(added, drops, errors)
    }

    suspend fun refreshAll(ctx: Context, includeCatalog: Boolean = true): Summary {
        val dao = HunterDb.get(ctx)
        val catalog = if (includeCatalog) searchCatalog(ctx, dao, ctx.getSharedPreferences("autopilot", 0).getString("catalogQuery", "").orEmpty())
            else Summary(emptyList(), emptyList(), emptyList())
        val newIds = catalog.newIds.toMutableList()
        val drops = catalog.dropIds.toMutableList()
        val errs = catalog.errors.toMutableList()
        for (s in dao.allSubs()) {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    http.newCall(Request.Builder().url(s.url).header("User-Agent", "RealtorAI/1.0").build()).execute().use { r ->
                        if (!r.isSuccessful) error("HTTP ${r.code}"); r.peekBody(5_000_000).string() }
                }
                val source = "feed:${s.name}"
                val rows = when(s.format) {
                    "json" -> HunterImport.fromJson(text, source)
                    "yrl" -> RealtyFeed.parse(text, source)
                    else -> HunterImport.fromCsv(text, source)
                }
                HunterImport.ingest(dao, rows)
            }.onSuccess { r -> newIds += r.newIds; drops += r.dropIds; dao.saveSub(s.copy(lastFetch = System.currentTimeMillis(), lastStatus = "OK: " + r.describe())) }
             .onFailure { e -> if (e is kotlinx.coroutines.CancellationException) throw e; errs += "${s.name}: ${e.message}"; dao.saveSub(s.copy(lastFetch = System.currentTimeMillis(), lastStatus = "Ошибка: ${e.message}")) }
        }
        return Summary(newIds, drops, errs)
    }

    @SuppressLint("MissingPermission")
    suspend fun notifyDeals(ctx: Context, s: Summary): Int {
        if (s.newIds.isEmpty() && s.dropIds.isEmpty()) return 0
        val dao = HunterDb.get(ctx); val all = dao.allOffers(); val cl = Dedup.clusters(all)
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
        runCatching { ru.vladis.realtorai.automation.LeadInbox.refresh(applicationContext) }.onFailure {
            if (it is kotlinx.coroutines.CancellationException) throw it
        }
        val s = HuntRun.refreshAll(applicationContext); if (ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(applicationContext)) ru.vladis.realtorai.autopilot.AutopilotEngine.sync(applicationContext, s.errors); HuntRun.notifyDeals(applicationContext, s); return Result.success()
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
