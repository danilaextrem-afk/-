package ru.vladis.realtorai.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.hunter.Offer
import ru.vladis.realtorai.publish.SaleIntake

@RunWith(AndroidJUnit4::class)
class SaleIntakeTest {
    @Test fun sharingAndTakingAnOfferReuseOneDraftWithoutAdvertisingApproval(): Unit = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).build()
        try {
            val url = "https://www.cian.ru/sale/flat/336656970/"
            val shared = SaleIntake.fromShare(db, "Объявление $url?utm_test=123")
            assertTrue(shared.pending)
            assertEquals(0L, db.publications().listing(shared.listingId)!!.price)
            val id = db.hunter().saveOffer(Offer(source = "fixture", externalId = "336656970", url = url,
                title = "Двухкомнатная квартира", address = "Москва, улица Лесная, 5", price = 10_000_000L,
                area = 50.0, rooms = 2, floor = 4, floorsTotal = 9))
            val taken = SaleIntake.fromOffer(db, id)
            assertEquals(shared.propertyId, taken.propertyId)
            assertEquals(shared.listingId, taken.listingId)
            val listing = db.publications().listing(taken.listingId)!!
            assertEquals(9, listing.floorsTotal)
            assertEquals(10_000_000L, listing.price)
            assertNull(listing.approvedAt)
            assertTrue(db.publications().pubsOf(listing.id).isEmpty())
            assertEquals(taken, SaleIntake.fromShare(db, url))
            assertEquals(1, db.dao().allProperties().size)
        } finally { db.close() }
    }
    @Test fun changedPriceAndSaleStayPendingUntilEachPlatformConfirms(): Unit = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).build()
        try {
            val dao = db.publications()
            val id = dao.saveListing(ru.vladis.realtorai.publish.Listing())
            dao.savePub(ru.vladis.realtorai.publish.Publication(listingId = id, platform = "TELEGRAM",
                status = "published", externalId = "19"))
            dao.savePub(ru.vladis.realtorai.publish.Publication(listingId = id, platform = "CIAN", status = "exported"))
            ru.vladis.realtorai.publish.PublicationSync.stage(dao, id, sold = false)
            assertTrue(dao.pubsOf(id).all { it.status == "outdated" })
            ru.vladis.realtorai.publish.PublicationSync.stage(dao, id, sold = true)
            val pending = dao.pubsOf(id)
            assertTrue(pending.all { it.status == "removal_pending" })
            assertEquals("19", pending.single { it.platform == "TELEGRAM" }.externalId)
            assertTrue(pending.single { it.platform == "CIAN" }.message.contains("фид"))
        } finally { db.close() }
    }
}
