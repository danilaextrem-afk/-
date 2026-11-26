package ru.vladis.realtorai.owner

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import ru.vladis.realtorai.Reminder
import ru.vladis.realtorai.data.*
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class OwnerService(private val context: Context) {
    private val db = WorkspaceDb.get(context)
    private val dao = db.owners()

    suspend fun start(id: Long) = db.withTransaction {
        requireNotNull(db.dao().property(id)) { "Объект удалён" }
        if (dao.get(id) == null) {
            val now = System.currentTimeMillis()
            dao.save(OwnerCase(id, OwnerStep.CALL.name, now, null, 1, .95, 1.0, 1.05, "", null, "", "", "", null, null, null, null, null))
            dao.event(OwnerEvent(propertyId = id, at = now, kind = "stage", text = "Начато сопровождение"))
        }
    }.also { schedule(context); syncTasks() }

    suspend fun advance(id: Long, expected: OwnerStep, evidence: String) {
        require(evidence.isNotBlank() && evidence.length <= 4000)
        val finishedTasks = db.withTransaction {
            val current = requireNotNull(dao.get(id))
            check(current.step == expected.name && expected != OwnerStep.FINISHED) { "Этап уже изменился" }
            val step = OwnerRules.next(expected)
            val now = System.currentTimeMillis()
            dao.save(current.copy(step = step.name, activatedAt = if (expected == OwnerStep.ACTIVATION) now else current.activatedAt))
            dao.event(OwnerEvent(propertyId = id, at = now, kind = "stage", text = "${expected.title}: ${evidence.trim()}"))
            val kind = "owner/$id/stage"
            db.dao().allTasks().filter { it.kind == kind && !it.done }.onEach { db.dao().saveTask(it.copy(done = true)) }
        }
        finishedTasks.forEach { Reminder.cancel(context, it.id) }
        syncTasks()
    }

    suspend fun update(value: OwnerCase) {
        require(value.reportsPerWeek in 1..2 && PriceStrategy(value.fast, value.normal, value.patient).valid())
        require(listOf(value.calls, value.leads, value.views, value.ownerCalls, value.impressions).all { it == null || it >= 0 })
        db.withTransaction {
            val old = requireNotNull(dao.get(value.id))
            dao.save(value.copy(step = old.step, startedAt = old.startedAt, activatedAt = old.activatedAt, lastReportAt = old.lastReportAt))
            val metrics = "Звонки: ${OwnerRules.metric(value.calls)}, обращения: ${OwnerRules.metric(value.leads)}, показы: ${OwnerRules.metric(value.views)}, просмотры рекламы: ${OwnerRules.metric(value.impressions)}, входящие у собственника: ${OwnerRules.metric(value.ownerCalls)}"
            dao.event(OwnerEvent(propertyId = value.id, at = System.currentTimeMillis(), kind = "work", text =
                listOf(metrics, value.advertising, value.packaging).filter(String::isNotBlank).joinToString("\n")))
        }
        syncTasks()
    }

    suspend fun recordReport(id: Long, text: String) {
        require(text.isNotBlank() && text.length <= 12000)
        db.withTransaction {
            val case = requireNotNull(dao.get(id))
            val now = System.currentTimeMillis()
            dao.event(OwnerEvent(propertyId = id, at = now, kind = "report", text = text))
            dao.save(case.copy(lastReportAt = now))
            db.dao().allTasks().filter { it.kind == "owner/$id/report" && !it.done }
                .forEach { db.dao().saveTask(it.copy(done = true)) }
        }
        syncTasks()
    }

    suspend fun changePrice(id: Long, expected: Long, price: Long) = db.withTransaction {
        require(price in 1..1_000_000_000_000L)
        val p = requireNotNull(db.dao().property(id))
        check(p.price == expected) { "Цена изменилась. Откройте подтверждение заново." }
        if (price != expected) {
            db.dao().saveProperty(p.copy(price = price))
            dao.event(OwnerEvent(propertyId = id, at = System.currentTimeMillis(), kind = "price", text = "${p.price} ₽ → $price ₽. Изменено в CRM."))
        }
    }

    suspend fun syncTasks() {
        val changes = db.withTransaction {
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val tasks = db.dao().allTasks().toMutableList()
            val scheduled = mutableListOf<TaskItem>()
            val cases = dao.active()
            val active = cases.mapTo(mutableSetOf()) { it.id }
            val closed = mutableListOf<Long>()
            for (task in tasks.filter { !it.done && it.kind.startsWith("owner/") }) {
                if (task.kind.split('/').getOrNull(1)?.toLongOrNull() !in active) {
                    db.dao().saveTask(task.copy(done = true))
                    closed += task.id
                }
            }
            suspend fun ensure(kind: String, title: String, due: Long, repeat: Boolean = false) {
                val open = tasks.firstOrNull { it.kind == kind && !it.done }
                if (open == null && !repeat && tasks.any { it.kind == kind }) return
                if (open?.title == title && open.dueAt == due) return
                val item = TaskItem(id = open?.id ?: 0, title = title, kind = kind, dueAt = due)
                scheduled += item.copy(id = db.dao().saveTask(item))
            }
            for (case in cases) {
                val property = db.dao().property(case.id) ?: continue
                val step = OwnerStep.valueOf(case.step)
                val stageKey = "owner/${case.id}/stage"
                val label = "${step.title} · ${property.title}"
                if (tasks.none { it.kind == stageKey && it.title == label }) ensure(stageKey, label, now, true)
                case.activatedAt?.let { activated ->
                    ensure("owner/${case.id}/review", "Аналитика первой недели · ${property.title}", activated + 7 * OwnerRules.day)
                    val due = OwnerRules.reportDue(case.lastReportAt ?: activated, case.reportsPerWeek, zone)
                    ensure("owner/${case.id}/report", "Отчёт собственнику · ${property.title}", due, true)
                }
            }
            scheduled to closed
        }
        changes.second.forEach { Reminder.cancel(context, it) }
        changes.first.forEach { Reminder.schedule(context, it.id, it.title, it.dueAt) }
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("owner-regimen",
                ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<OwnerWorker>(12, TimeUnit.HOURS).build())
        }
    }
}

class OwnerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        OwnerService(applicationContext).syncTasks()
        return Result.success()
    }
}
