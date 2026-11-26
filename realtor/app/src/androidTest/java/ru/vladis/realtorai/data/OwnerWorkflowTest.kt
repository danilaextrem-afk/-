package ru.vladis.realtorai.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.owner.OwnerService
import ru.vladis.realtorai.owner.OwnerStep

@RunWith(AndroidJUnit4::class)
class OwnerWorkflowTest {
    @Test fun activationTasksReportsAndConfirmedPriceSurviveRepeatedCalls(): Unit = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = WorkspaceDb.get(ctx)
        val service = OwnerService(ctx)
        val id = db.dao().saveProperty(Property(title = "Квартира интеграционного теста", price = 10_000_000L))
        try {
            service.start(id)
            service.start(id)
            assertEquals(1, db.dao().allTasks().count { it.kind == "owner/$id/stage" && !it.done })
            service.advance(id, OwnerStep.CALL, "Собственник ответил")
            assertTrue(runCatching { service.advance(id, OwnerStep.CALL, "Повтор") }.isFailure)
            service.advance(id, OwnerStep.AGREEMENT, "Условия согласованы")
            service.advance(id, OwnerStep.ACTIVATION, "Карточка проверена")
            service.syncTasks()
            assertEquals(1, db.dao().allTasks().count { it.kind == "owner/$id/review" })
            assertEquals(1, db.dao().allTasks().count { it.kind == "owner/$id/report" && !it.done })
            service.recordReport(id, "Собран отчёт. Показы: нет данных")
            assertEquals(1, db.dao().allTasks().count { it.kind == "owner/$id/report" && !it.done })
            assertTrue(runCatching { service.changePrice(id, 1, 9_500_000) }.isFailure)
            assertEquals(10_000_000L, db.dao().property(id)?.price)
            service.changePrice(id, 10_000_000, 9_500_000)
            assertEquals(9_500_000L, db.dao().property(id)?.price)
            assertEquals(1, db.owners().history(id).first().count { it.kind == "price" })
        } finally {
            db.dao().allTasks().filter { it.kind.startsWith("owner/$id/") }.forEach { db.dao().deleteTask(it) }
            db.owners().clearEvents(id)
            db.owners().delete(id)
            db.dao().property(id)?.let { db.dao().deleteProperty(it) }
        }
    }
}
