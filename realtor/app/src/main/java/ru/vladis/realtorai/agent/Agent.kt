package ru.vladis.realtorai.agent

import android.content.Context
import com.google.gson.*
import ru.vladis.realtorai.ai.DeviceModel
import ru.vladis.realtorai.publish.Listing
import ru.vladis.realtorai.publish.SmartAi
import ru.vladis.realtorai.data.TaskItem
import ru.vladis.realtorai.Reminder
import androidx.room.*
import androidx.room.withTransaction
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import ru.vladis.realtorai.autopilot.AutoRules
import ru.vladis.realtorai.autopilot.AutopilotEngine
import ru.vladis.realtorai.data.AppDb
import ru.vladis.realtorai.data.WorkspaceDb
import ru.vladis.realtorai.hunter.HuntRun
import ru.vladis.realtorai.hunter.GeoScope
import ru.vladis.realtorai.hunter.GeoPreferences
import ru.vladis.realtorai.hunter.Offer
import ru.vladis.realtorai.hunter.Dedup
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
    @Query("UPDATE AgentJob SET plan=:plan, result=:result, state=:state WHERE id=:id AND state='running'")
    suspend fun checkpoint(id: Long, plan: String, result: String, state: String): Int
    @Query("SELECT * FROM AgentJob WHERE state='done' AND clientId IS :client ORDER BY id DESC LIMIT 3")
    suspend fun context(client: Long?): List<AgentJob>
    @Query("UPDATE AgentJob SET goal=:goal, state='queued' WHERE id=:id AND state='waiting'")
    suspend fun answer(id: Long, goal: String): Int
    @Query("UPDATE AgentJob SET state=:next, result=:result WHERE id=:id AND state=:expected")
    suspend fun transition(id: Long, expected: String, next: String, result: String = ""): Int
}

enum class AgentTool(val label: String, val network: Boolean = false) {
    FEEDS("Обновить подключённые источники и импортировать объекты", true),
    MATCH("Подобрать объекты по критериям клиента"),
    MARKET("Сводка по ценам в подключённых источниках"),
    LEADS("Показать контакты из разрешённых импортов"),
    WEEK("План задач на неделю"),
    OWNERS("Обновить задачи сопровождения собственников"),
    FIND("Найти предложения в базе"),
    OWNER_REPORT("Подготовить отчёт собственнику"),
    LISTING("Подготовить черновик объявления"),
    DOCUMENT("Составить предложение или сообщение"),
    TASK("Запланировать локальную задачу")
}

object AgentPlan {
    fun fromModel(text: String): List<AgentTool> {
        val trimmed = text.trim()
        require(trimmed.length <= 80) { "Ответ модели не соответствует формату плана" }
        val tokens = trimmed.split(',').map(String::trim)
        require(tokens.size in 1..4 && tokens.distinct().size == tokens.size) { "Некорректный план" }
        return tokens.map { token -> AgentTool.entries.firstOrNull { it.name == token } ?: error("Инструмент недоступен") }
    }

    fun fromGoal(goal: String): List<AgentTool> {
        val text = goal.lowercase().replace('ё', 'е')
        return buildList {
            if (listOf("найди", "поиск", "обнови").any(text::contains) &&
                listOf("квартир", "объект", "источник", "клиент", "собственник", "сделк", "двуш", "однуш", "треш").any(text::contains)) {
                add(AgentTool.FEEDS)
                if (ListingQuery.read(goal) != ListingQuery()) add(AgentTool.FIND)
            }
            if ("подбер" in text || "подбор" in text || "бюджет клиента" in text) add(AgentTool.MATCH)
            if (listOf("рынок", "цен", "конкурент", "выгодн").any(text::contains)) add(AgentTool.MARKET)
            if ("контакт" in text || "новых клиентов" in text || "покупателей" in text) add(AgentTool.LEADS)
            if ("недел" in text || "план" in text) add(AgentTool.WEEK)
            if ("отчет" in text || "сопровожден" in text) add(AgentTool.OWNERS)
        }
    }
}

internal data class AgentAction(val tool: AgentTool, val args: JsonObject)
internal data class AgentProgress(val adaptive: Boolean, val pending: List<AgentAction>, val steps: List<String>, val used: List<AgentAction>? = null)
internal data class AgentDecision(val action: AgentAction?, val question: String? = null, val finish: String? = null)

internal object AgentProtocol {
    private val gson = Gson()
    fun load(value: String): AgentProgress = when {
        value == "adaptive" -> AgentProgress(true, emptyList(), emptyList())
        value.startsWith("{") -> gson.fromJson(value, AgentProgress::class.java)
        else -> AgentProgress(false, value.split(',').map { AgentAction(AgentTool.valueOf(it), JsonObject()) }, emptyList())
    }
    fun save(progress: AgentProgress) = gson.toJson(progress)
    fun parse(raw: String): AgentDecision {
        val text = raw.substringAfterLast("</think>").trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        require(text.length <= 2200)
        val value = JsonParser.parseString(text).asJsonObject
        require(value.keySet().singleOrNull() in setOf("ask","finish") || value.keySet() == setOf("tool","args"))
        if (value.has("ask")) return AgentDecision(null, question = value.get("ask").asString.trim().also { require(it.length in 4..350) })
        if (value.has("finish")) return AgentDecision(null, finish = value.get("finish").asString.take(800))
        val tool = AgentTool.valueOf(value.get("tool").asString)
        val args = value.getAsJsonObject("args")
        val allowed = when (tool) {
            AgentTool.FIND -> setOf("area","budget","rooms","minArea","market","seller","address","station")
            AgentTool.MATCH -> setOf("client")
            AgentTool.OWNER_REPORT, AgentTool.LISTING -> setOf("property")
            AgentTool.DOCUMENT -> setOf("title")
            AgentTool.TASK -> setOf("title","date")
            else -> emptySet()
        }
        require(args.keySet().all { it in allowed } && args.entrySet().all { it.value.isJsonPrimitive || it.value.isJsonNull })
        args.entrySet().removeIf { it.value.isJsonNull }
        return AgentDecision(AgentAction(tool,args))
    }
}

class AgentQueue(private val context: Context) {
    private val dao = WorkspaceDb.get(context).agent()
    private val work = WorkManager.getInstance(context)
    suspend fun submit(goal: String, plan: List<AgentTool>, client: Long?) {
        require(goal.isNotBlank() && goal.length <= 500 && plan.isNotEmpty() && plan.size <= AgentTool.entries.size && plan.distinct().size == plan.size)
        require(AgentTool.MATCH !in plan || client != null && client > 0)
        enqueue(dao.insert(AgentJob(goal = goal, plan = plan.joinToString(",") { it.name }, clientId = client)), plan.any { it.network })
    }
    suspend fun adaptive(goal: String, client: Long?) {
        require(goal.isNotBlank() && goal.length <= 500)
        require(DeviceModel.installed(context)) { "Скачайте локальную модель" }
        enqueue(dao.insert(AgentJob(goal = goal, plan = "adaptive", clientId = client)), false)
    }
    suspend fun answer(job: AgentJob, reply: String) {
        require(reply.isNotBlank())
        if (dao.answer(job.id, job.goal.takeLast(900) + "\nУточнение: " + reply.trim().take(350)) > 0) enqueue(job.id, false)
    }
    suspend fun cancel(job: AgentJob) {
        val updated = dao.transition(job.id, "queued", "cancelled", job.result) + dao.transition(job.id, "running", "cancelled", job.result)
        if (updated > 0) work.cancelUniqueWork("agent-${job.id}")
    }
    suspend fun resume(job: AgentJob) {
        if (job.state !in listOf("failed", "cancelled")) return
        if (dao.transition(job.id, job.state, "queued", job.result) > 0) enqueue(job.id, AgentTool.FEEDS.name in job.plan)
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
            var progress = AgentProtocol.load(job.plan)
            while (!isStopped && dao.get(job.id)?.state == "running") {
                if (progress.pending.isEmpty()) {
                    if (!progress.adaptive || progress.steps.size >= 8) {
                        val result = progress.steps + if (progress.adaptive) listOf("Выполнено восемь шагов. Проверьте результаты поручения.") else emptyList()
                        dao.checkpoint(job.id, AgentProtocol.save(progress), result.joinToString("\n\n"), "done")
                        return Result.success()
                    }
                    val prior = job.clientId?.let { dao.context(it).firstOrNull()?.result }.orEmpty().take(160)
                    val decision = decide(job, progress, prior)
                    if (decision.action == null) {
                        val message = decision.question ?: decision.finish.orEmpty()
                        dao.checkpoint(job.id, AgentProtocol.save(progress), (progress.steps + message).joinToString("\n\n"),
                            if (decision.question != null) "waiting" else "done")
                        return Result.success()
                    }
                    progress = progress.copy(pending = listOf(decision.action))
                    if (dao.checkpoint(job.id, AgentProtocol.save(progress), progress.steps.joinToString("\n\n"), "running") == 0) return Result.success()
                }
                val action = progress.pending.first()
                val report = execute(action, db, job, progress.steps.size)
                progress = progress.copy(pending = progress.pending.drop(1),
                    steps = progress.steps + (action.tool.label + "\n" + report.take(2200)), used = progress.used.orEmpty() + action)
                if (dao.checkpoint(job.id, AgentProtocol.save(progress), progress.steps.joinToString("\n\n"), "running") == 0) return Result.success()
            }
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (runAttemptCount < 2) {
                dao.transition(job.id, "running", "queued", (dao.get(job.id)?.result.orEmpty() + "\n" + e.localizedMessage.orEmpty()).take(20000))
                Result.retry()
            } else {
                dao.transition(job.id, "running", "failed", (dao.get(job.id)?.result.orEmpty() + "\n" + e.localizedMessage.orEmpty()).take(20000))
                Result.failure()
            }
        }
    }

    private suspend fun decide(job: AgentJob, progress: AgentProgress, prior: String): AgentDecision {
        val system = """Один следующий шаг JSON: {"tool":"КОД","args":{}} или {"ask":"вопрос"} или {"finish":"итог"}. FEEDS обновить; FIND {area,budget,rooms,minArea,market,seller,address,station}: seller owner/developer, только Москва и МО; MATCH {client}; MARKET цены; LEADS заявки; WEEK задачи; OWNERS сроки; OWNER_REPORT {property}; LISTING {property}; DOCUMENT {title}; TASK {title,date} YYYY-MM-DD. ID числовые. Смотри фактические результаты, не повторяй выполненное. Передавай только известные параметры. Если критически не хватает данных, ask."""
        val observed = progress.steps.zip(progress.used.orEmpty()) { result, call ->
            "${call.tool} ${call.args}: ${result.substringAfter('\n').take(180)}"
        }.joinToString("\n").takeLast(570)
        val brief = "Сегодня: ${LocalDate.now()}. Цель: ${job.goal.takeLast(440)}. Клиент ID: ${job.clientId ?: "нет"}.\nВыполнено:\n$observed\nПамять: $prior"
        fun validate(raw: String): AgentDecision = AgentProtocol.parse(raw).also {
            require(it.action == null || it.action !in progress.used.orEmpty()) { "Модель повторила выполненный вызов" }
        }
        val first = DeviceModel.answer(applicationContext, system, listOf("user" to brief))
        return try { validate(first) } catch (_: RuntimeException) {
            val correction = "Последний ответ недопустим или повторяет выполненное. Верни следующий шаг, уточнение или итог.\n"
            validate(DeviceModel.answer(applicationContext, system, listOf("user" to (correction + brief).take(1200))))
        }
    }

    private suspend fun execute(action: AgentAction, db: AppDb, job: AgentJob, index: Int): String {
        fun arg(name: String) = action.args.get(name)?.asString.orEmpty()
        fun number(name: String) = arg(name).takeIf(String::isNotBlank)?.toLong()
        val ctx = applicationContext
        return when (action.tool) {
        AgentTool.FIND -> ListingSearch.run(ctx, db, job.goal, action.args)
        AgentTool.OWNER_REPORT -> OwnerService(ctx).draftReport(requireNotNull(number("property")) { "Нужен ID объекта" })
        AgentTool.LISTING -> db.withTransaction {
            val id = requireNotNull(number("property")) { "Нужен ID объекта" }
            val p = requireNotNull(db.dao().property(id)) { "Объект не найден" }
            val existing = db.publications().listingForProperty(id)
            if (existing != null) "Карточка #${existing.id} уже есть · ${existing.status}"
            else {
                val draft = Listing(propertyId = id, title = p.title.take(50), address = p.address,
                    price = p.price, area = p.area, rooms = p.rooms, floor = p.floor, market = p.type)
                val listingId = db.publications().saveListing(draft.copy(description = SmartAi.factsText(draft)))
                "Черновик #$listingId сохранён в Smart Publish"
            }
        }
        AgentTool.DOCUMENT -> DeviceModel.answer(ctx, "Подготовь черновик документа по-русски. Используй только переданные факты.", listOf("user" to
            "${arg("title").take(80)}\n${job.goal.take(380)}\n${db.agent().get(job.id)?.result.orEmpty().takeLast(600)}"))
        AgentTool.TASK -> {
            val title = arg("title").trim().take(120)
            require(title.isNotEmpty())
            val date = LocalDate.parse(arg("date"))
            require(!date.isBefore(LocalDate.now())) { "Дата задачи уже прошла" }
            val due = date.atTime(10,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val task = db.withTransaction {
                val kind = "agent/${job.id}/$index"
                db.dao().allTasks().firstOrNull { it.kind == kind } ?: TaskItem(title = title, kind = kind, dueAt = due)
                    .let { it.copy(id = db.dao().saveTask(it)) }
            }
            Reminder.schedule(ctx, task.id, task.title, task.dueAt)
            "Задача #${task.id} · ${task.title} · $date"
        }
        AgentTool.FEEDS -> {
            val search = HuntRun.refreshAll(applicationContext)
            AutopilotEngine.sync(applicationContext, search.errors).summary()
        }
        AgentTool.MATCH -> {
            val client = db.dao().allClients().firstOrNull { it.id == (number("client") ?: job.clientId) } ?: error("Клиент отсутствует в CRM")
            AutoRules.matches(client, GeoScope.searchableProperties(db.dao().allProperties(), db.hunter().allOffers(), GeoPreferences.read(ctx))).joinToString("\n") {
                "${it.title} · ${it.price.rub()} · ${it.address}\n${it.url}"
            }.ifBlank { "В CRM нет объектов по подтверждённым критериям клиента." }
        }
        AgentTool.MARKET -> {
            val geography = GeoPreferences.read(ctx)
            val sales = db.hunter().allOffers().filter { GeoScope.eligible(it) && geography.matches(it) && it.deal == "sale" && it.price > 0 && it.area.isFinite() && it.area > 0 }
            sales.groupBy { it.district.ifBlank { "Район не указан" } to it.market }.entries.sortedByDescending { it.value.size }
                .take(8).joinToString("\n") { (group, entries) ->
                    val prices = entries.map(Market::ppsm).sorted()
                    "${group.first} · ${group.second} · ${prices.size} объявлений · медиана выборки ${prices[prices.size / 2].rub()}/м²"
                }.ifBlank { "Нет объявлений с подтверждённой ценой и площадью." }
        }
        AgentTool.LEADS -> ru.vladis.realtorai.automation.LeadInbox.refresh(ctx)
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
}
