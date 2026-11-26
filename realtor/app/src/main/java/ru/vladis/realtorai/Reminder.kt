package ru.vladis.realtorai

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(ctx: Context, p: WorkerParameters) : Worker(ctx, p) {
    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("tasks", "Напоминания", NotificationManager.IMPORTANCE_HIGH))
        val n = NotificationCompat.Builder(applicationContext, "tasks")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Realtor AI").setContentText(inputData.getString("title") ?: "Задача")
            .setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(System.nanoTime().toInt(), n) }
        return Result.success()
    }
}

object Reminder {
    /** Напоминание за 15 минут до срока */
    fun schedule(ctx: Context, id: Long, title: String, at: Long) {
        if (at < System.currentTimeMillis()) return
        val delay = (at - 15 * 60_000 - System.currentTimeMillis()).coerceAtLeast(0)
        WorkManager.getInstance(ctx).enqueueUniqueWork("task-$id", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("title" to title)).build())
    }
    fun cancel(ctx: Context, id: Long) { WorkManager.getInstance(ctx).cancelUniqueWork("task-$id") }
}
