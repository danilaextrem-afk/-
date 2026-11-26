package ru.vladis.realtorai.agent

import android.content.Context
import androidx.room.*
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import ru.vladis.realtorai.autopilot.AutoRules
import ru.vladis.realtorai.autopilot.AutopilotEngine
import ru.vladis.realtorai.data.AppDb
import ru.vladis.realtorai.data.WorkspaceDb
import ru.vladis.realtorai.hunter.HuntRun
import ru.vladis.realtorai.hunter.Market
import ru.vladis.realtorai.owner.OwnerService
import ru.vladis.realtorai.ui.rub
import java.time.LocalDate
import java.time.ZoneId

@Entity
data class AgentJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: String,
    val plan: String,
    val clientId: Long?,
    val state: String = "queued",
    val result: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao interface AgentDao {
    @Insert suspend fun insert(job: AgentJob): Long
    @Query("SELECT * FROM AgentJob ORDER BY id DESC LIMIT 40") fun recent(): Flow<List<AgentJob>>
    @Query("SELECT * FROM AgentJob WHERE id=:id") suspend fun get(id: Long): AgentJob?
    @Query("UPDATE AgentJob SET state=:next, result=:result WHERE id=:id AND state=:expected")
    suspend fun transition(id: Long, expected: String, next: String, result: String = ""): Int
}

enum class AgentTool(val label: String, val network: Boolean = false) {
    FEEDS("Обновить подключённые источники и импортировать объекты", true),
    MATCH("Подобрать объекты по критериям клиента"),
    MARKET("Сводка по ценам в подключённых источниках"),
    LEADS("Показать контакты из разрешённых импортов"),
    WEEK("План задач на неделю"),
    OWNERS("Обновить задачи сопровождения собственников")
}

object AgentPlan {
    fun fromGoal(goal: String): List<AgentTool> {
        val text = goal.lowercase().replace('ё', 'е')
        return buildList {
            if (listOf("найди", "поиск", "обнови").any(text::contains) &&
                listOf("квартир", "объект", "источник", "клиент", "собственник", "сделк").any(text::contains)) add(AgentTool.FEEDS)
            if ("подбер" in text || "подбор" in text || "бюджет клиента" in text) add(AgentTool.MATCH)
            if (listOf("рынок", "цен", "конкурент", "выгодн").any(text::contains)) add(AgentTool.MARKET)
            if ("контакт" in text || "новых клиентов" in text || "покупателей" in text) add(AgentTool.LEADS)
            if ("недел" in text || "план" in text) add(AgentTool.WEEK)
            if ("отчет" in text || "сопровожден" in text) add(AgentTool.OWNERS)
        }
    }
}

class AgentQueue(private val context: Context) {
    private val dao = WorkspaceDb.get(context).agent()
    private val work = WorkManager.getInstance(context)
    suspend fun submit(goal: String, plan: List<AgentTool>, client: Long?) {
        require(goal.isNotBlank() && goal.length <= 500 && plan.isNotEmpty() && plan == AgentPlan.fromGoal(goal))
        require(AgentTool.MATCH !in plan || client != null && client > 0)
        enqueue(dao.insert(AgentJob(goal = goal, plan = plan.joinToString(",") { it.name }, clientId = client)), plan.any { it.network })
    }
    suspend fun cancel(job: AgentJob) {
        val updated = dao.transition(job.id, "queued", "cancelled") + dao.transition(job.id, "running", "cancelled")
        if (updated > 0) work.cancelUniqueWork("agent-${job.id}")
    }
    suspend fun resume(job: AgentJob) {
        if (job.state !in listOf("failed", "cancelled")) return
        if (dao.transition(job.id, job.state, "queued") > 0) enqueue(job.id, AgentTool.FEEDS.name in job.plan)
    }
    private fun enqueue(id: Long, network: Boolean) {
        val constraints = Constraints.Builder().setRequiredNetworkType(if (network) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
        work.enqueueUniqueWork("agent-$id", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AgentWorker>().setConstraints(constraints.build())
                .setInputData(workDataOf("id" to id)).build())
    }
}

class AgentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val db = WorkspaceDb.get(context)
    override suspend fun doWork(): Result {
        val dao = db.agent()
        val job = dao.get(inputData.getLong("id", -1)) ?: return Result.failure()
        if (job.state !in listOf("queued", "running")) return Result.success()
        if (dao.transition(job.id, job.state, "running", job.result) == 0) return Result.success()
        return try {
            val tools = job.plan.split(',').map(AgentTool::valueOf)
            val sections = mutableListOf<String>()
            for (tool in tools) {
                if (isStopped || dao.get(job.id)?.state != "running") return Result.success()
                val report = execute(tool, db, job)
                sections += tool.label + "\n" + report
            }
            dao.transition(job.id, "running", "done", sections.joinToString("\n\n").take(18000))
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (runAttemptCount < 2) {
                dao.transition(job.id, "running", "queued", e.localizedMessage.orEmpty())
                Result.retry()
            } else {
                dao.transition(job.id, "running", "failed", e.localizedMessage.orEmpty())
                Result.failure()
            }
        }
    }

    private suspend fun execute(tool: AgentTool, db: AppDb, job: AgentJob): String = when (tool) {
        AgentTool.FEEDS -> {
            val search = HuntRun.refreshAll(applicationContext)
            AutopilotEngine.sync(applicationContext, search.errors).summary()
        }
        AgentTool.MATCH -> {
            val client = db.dao().allClients().firstOrNull { it.id == job.clientId } ?: error("Клиент отсутствует в CRM")
            AutoRules.matches(client, db.dao().allProperties()).joinToString("\n") {
                "${it.title} · ${it.price.rub()} · ${it.address}\n${it.url}"
            }.ifBlank { "В CRM нет объектов по подтверждённым критериям клиента." }
        }
        AgentTool.MARKET -> {
            val sales = db.hunter().allOffers().filter { it.active && it.deal == "sale" && it.price > 0 && it.area.isFinite() && it.area > 0 }
            sales.groupBy { it.district.ifBlank { "Район не указан" } to it.market }.entries.sortedByDescending { it.value.size }
                .take(8).joinToString("\n") { (group, entries) ->
                    val prices = entries.map(Market::ppsm).sorted()
                    "${group.first} · ${group.second} · ${prices.size} объявлений · медиана выборки ${prices[prices.size / 2].rub()}/м²"
                }.ifBlank { "Нет объявлений с подтверждённой ценой и площадью." }
        }
        AgentTool.LEADS -> db.dao().allClients().filter { it.notes.startsWith("Источник: ") }
            .sortedByDescending { it.createdAt }.take(20).joinToString("\n") { "${it.name} · ${it.notes}" }
            .ifBlank { "В CRM нет контактов, полученных из подключённых источников." }
        AgentTool.WEEK -> {
            val end = LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.dao().allTasks().filter { !it.done && it.dueAt < end }.sortedBy { it.dueAt }.take(30)
                .joinToString("\n") { it.title }.ifBlank { "Открытых задач на ближайшую неделю нет." }
        }
        AgentTool.OWNERS -> {
            val cases = db.owners().active()
            OwnerService(applicationContext).syncTasks()
            "Проверено активных объектов: ${cases.size}. Сроки отчётов и задачи обновлены."
        }
    }
}
