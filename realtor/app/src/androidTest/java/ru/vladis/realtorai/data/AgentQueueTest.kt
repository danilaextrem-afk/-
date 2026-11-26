package ru.vladis.realtorai.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.agent.AgentPlan
import ru.vladis.realtorai.agent.AgentQueue

@RunWith(AndroidJUnit4::class)
class AgentQueueTest {
    @Test fun runsTwoLocalToolsInWorkManagerAndPersistsTheResult(): Unit = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = WorkspaceDb.get(ctx)
        val task = db.dao().saveTask(TaskItem(title = "Проверить договор QA", dueAt = System.currentTimeMillis() + 60_000))
        val goal = "Составь план на неделю и обнови сопровождение"
        try {
            AgentQueue(ctx).submit(goal, AgentPlan.fromGoal(goal), null)
            val jobs = withTimeout(60_000) {
                db.agent().recent().first { rows -> rows.any { it.goal == goal && it.state in listOf("done", "failed") } }
            }
            val job = jobs.first { it.goal == goal }
            assertEquals(job.result, "done", job.state)
            assertTrue(job.result.contains("Проверить договор QA"))
            assertTrue(job.result.contains("Проверено активных объектов:"))
        } finally { db.dao().deleteTask(TaskItem(id = task, title = "", dueAt = 0)) }
    }
    @Test fun resumesFromCheckpointWithoutRepeatingPriorStep(): Unit = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = WorkspaceDb.get(ctx).agent()
        val goal = "Проверка сохранённого этапа"
        val plan = """{"adaptive":false,"pending":[{"tool":"WEEK","args":{}}],"steps":["Предыдущий шаг обработан"]}"""
        val id = dao.insert(ru.vladis.realtorai.agent.AgentJob(goal = goal, plan = plan, clientId = null, state = "cancelled"))
        AgentQueue(ctx).resume(checkNotNull(dao.get(id)))
        val completed = withTimeout(60_000) {
            dao.recent().first { jobs -> jobs.any { it.id == id && it.state in setOf("done", "failed") } }.first { it.id == id }
        }
        assertEquals(completed.result, "done", completed.state)
        assertEquals(1, Regex("Предыдущий шаг обработан").findAll(completed.result).count())
        assertTrue(completed.result.contains("План задач на неделю"))
    }
}
