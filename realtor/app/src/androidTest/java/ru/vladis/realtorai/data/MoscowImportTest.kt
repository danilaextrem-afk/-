package ru.vladis.realtorai.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.hunter.*

@RunWith(AndroidJUnit4::class)
class MoscowImportTest {
    @Test fun offlineBoundariesGateDatabaseImportAndMeasureMkad() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = WorkspaceDb.get(context)
        val moscow = Offer(source="geo-fixture", externalId="msk", lat=55.750, lon=37.62, price=10_000_000, area=44.0)
        val oblast = moscow.copy(externalId="mo", address="Клин, Лесная", lat=56.331, lon=36.729)
        val other = moscow.copy(externalId="other", address="Москва, Тверская", lat=54.193, lon=37.619)
        val unknown = moscow.copy(externalId="unknown", lat=null, lon=null)
        try {
            val result = HunterImport.ingest(db.hunter(), listOf(moscow, oblast, other, unknown))
            val rows = db.hunter().allOffers().filter { it.source == "geo-fixture" }
            assertEquals(2, result.newIds.size)
            assertEquals(2, result.skipped)
            assertEquals(setOf(GeoScope.MOSCOW,GeoScope.OBLAST), rows.mapNotNull { it.region }.toSet())
            assertTrue(requireNotNull(GeoScope.mkadKm(GeoScope.Point(55.750,37.62))) in 10.0..30.0)
        } finally { db.hunter().allOffers().filter { it.source == "geo-fixture" }.forEach {
            db.hunter().deletePoints(it.id)
            db.hunter().deleteOffer(it.id)
        } }
    }
}
