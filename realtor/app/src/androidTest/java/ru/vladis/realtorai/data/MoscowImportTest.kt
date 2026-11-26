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
            val newMoscow = moscow.copy(lat=55.594066, lon=37.458299)
            assertTrue(GeoQuery(locations=listOf("Новая Москва")).matches(newMoscow))
            assertFalse(GeoQuery(locations=listOf("Новая Москва")).matches(moscow))
            assertTrue(requireNotNull(GeoScope.mkadKm(GeoScope.Point(55.750,37.62))) in 10.0..30.0)
        } finally { db.hunter().allOffers().filter { it.source == "geo-fixture" }.forEach {
            db.hunter().deletePoints(it.id)
            db.hunter().deleteOffer(it.id)
        } }
    }
    @Test fun authorizedPartnerFeedReadsSalesMetadata() {
        val avito = """<Ads formatVersion="3"><Ad><Id>one</Id><Category>Квартиры</Category>
            <OperationType>Продам</OperationType><Address>г. Москва, Лестева, 2</Address><Region>Москва</Region>
            <Price>13000000</Price><Square>45.1</Square><Rooms>2</Rooms><SellerType>owner</SellerType></Ad></Ads>"""
        val cian = """<feed><object><Category>newBuildingFlatSale</Category><ExternalId>two</ExternalId>
            <Address>Химки, Новая, 3</Address><Region>Московская область</Region><TotalArea>46</TotalArea>
            <BargainTerms><Price>14000000</Price><Currency>rur</Currency></BargainTerms></object></feed>"""
        val first = PartnerFeed.parse(avito,"partner","avito").single()
        val second = PartnerFeed.parse(cian,"developer","cian").single()
        assertEquals("owner",first.seller)
        assertEquals(GeoScope.MOSCOW,GeoScope.region(first))
        assertEquals("primary",second.market)
        assertEquals(GeoScope.OBLAST,GeoScope.region(second))
    }
}
