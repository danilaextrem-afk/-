package ru.vladis.realtorai.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.publish.*
import ru.vladis.realtorai.hunter.*
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceBackupTest {
    @Test fun restoresLinkedRecordsAndPhotosAndRejectsPartialLegacyBackup(): Unit = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).build()
        val photoDir = File(ctx.filesDir, "publish/121").apply { mkdirs() }
        val photo = File.createTempFile("original-", ".jpg", photoDir)
        try {
            photo.writeBytes(byteArrayOf(3, 6, 9))
            val buyer = Client(id = 51, name = "Ирина", desiredKind = "Квартира", wishes = "лоджия")
            db.dao().saveClient(buyer)
            db.dao().saveProperty(Property(id = 77, title = "Квартира"))
            db.hunter().saveOffer(Offer(id = 91, source = "catalog"))
            db.hunter().savePoint(PricePoint(offerId = 91, price = 1000))
            db.publications().saveListing(Listing(id = 121, propertyId = 77, photos = photo.path, cover = photo.path))
            db.publications().savePub(Publication(listingId = 121, platform = "test", status = "exported"))
            db.dao().log(RunLog(summary = "Поиск"))
            db.owners().event(ru.vladis.realtorai.owner.OwnerEvent(propertyId = 77, at = 11, kind = "report", text = "Первый отчёт"))
            db.agent().insert(ru.vladis.realtorai.agent.AgentJob(goal = "Часть очереди", plan = "WEEK", clientId = null))
            val archive = ByteArrayOutputStream()
            WorkspaceBackup.write(ctx, db, archive)
            photo.delete()
            db.clearAllTables()
            WorkspaceBackup.read(ctx, db, archive.toByteArray().inputStream())
            assertEquals(buyer, db.dao().allClients().single())
            val listing = db.publications().listing(121)!!
            assertEquals(listing.cover, listing.photos)
            assertArrayEquals(byteArrayOf(3, 6, 9), File(listing.cover).readBytes())
            assertEquals("exported", db.publications().pubsOf(121).single().status)
            assertEquals(91L, db.hunter().points().first().single().offerId)
            assertEquals("Поиск", db.dao().recentRuns().first().single().summary)
            assertEquals("Первый отчёт", db.owners().history(77).first().single().text)
            assertEquals("cancelled", db.agent().recent().first().single().state)
            val bad = """{"clients":[],"properties":[],"interactions":[],"tasks":[{"id":1}]}"""
            assertTrue(runCatching { WorkspaceBackup.read(ctx, db, bad.byteInputStream()) }.isFailure)
            assertEquals(buyer, db.dao().allClients().single())
            assertTrue(File(listing.cover).delete())
        } finally { photo.delete(); db.close() }
    }
}
