package ru.vladis.realtorai.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.autopilot.AutopilotEngine
import ru.vladis.realtorai.hunter.HunterImport

@RunWith(AndroidJUnit4::class)
class AutopilotIntegrationTest {
    @Test fun repeatsFeedWithoutDuplicateOwnersPropertiesDraftsOrFollowups() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = WorkspaceDb.get(ctx)
        val settings = ctx.getSharedPreferences("autopilot", 0)
        settings.edit().putBoolean("enabled", true).commit()
        try {
            val csv = "id;address;price;area;rooms;owner_name;owner_phone;crm_allowed\n" +
                "e2e;Москва Лесная 10;6500000;52;2;Тест;79000000000;true"
            repeat(2) {
                HunterImport.ingest(db.hunter(), HunterImport.fromCsv(csv, "integration"))
                AutopilotEngine.sync(ctx)
            }
            val owner = db.dao().allClients().single { it.phone == "79000000000" }
            val property = db.dao().allProperties().single { it.address == "Москва Лесная 10" }
            assertEquals(owner.id, property.ownerId)
            val listing = db.publications().listings().first().single { it.propertyId == property.id }
            assertEquals("draft", listing.status)
            assertTrue(db.publications().pubsOf(listing.id).isEmpty())
            assertEquals(1, db.dao().allTasks().count { it.kind.startsWith("followup/${owner.id}/") })
            assertEquals(2, db.dao().recentRuns().first().size)
        } finally {
            settings.edit().remove("enabled").commit()
            db.clearAllTables()
        }
    }
}
