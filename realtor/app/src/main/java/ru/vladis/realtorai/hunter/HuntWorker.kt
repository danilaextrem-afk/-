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

    /** Обновляет все подписки на фиды. Если подписок нет — ничего не делает (данные только из импорта). */
    suspend fun refreshAll(ctx: Context): Summary {
        val dao = HunterDb.get(ctx); val newIds = mutableListOf<Long>(); val drops = mutableListOf<Long>(); val errs = mutableListOf<String>()
        for (s in dao.allSubs()) {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    http.newCall(Request.Builder().url(s.url).header("User-Agent", "RealtorAI/1.0").build()).execute().use { r ->
                        if (!r.isSuccessful) error("HTTP ${r.code}"); r.peekBody(5_000_000).string() }
                }
                val rows = if (s.format == "json") HunterImport.fromJson(text, "feed:${s.name}") else HunterImport.fromCsv(text, "feed:${s.name}")
                HunterImport.ingest(dao, rows)
            }.onSuccess { r -> newIds += r.newIds; drops += r.dropIds; dao.saveSub(s.copy(lastFetch = System.currentTimeMillis(), lastStatus = "OK: " + r.describe())) }
             .onFailure { e -> errs += "${s.name}: ${e.message}"; dao.saveSub(s.copy(lastFetch = System.currentTimeMillis(), lastStatus = "Ошибка: ${e.message}")) }
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
