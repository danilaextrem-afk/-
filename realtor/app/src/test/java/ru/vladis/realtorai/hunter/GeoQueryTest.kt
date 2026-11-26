package ru.vladis.realtorai.hunter

import org.junit.Assert.*
import org.junit.Test
import ru.vladis.realtorai.agent.ListingQuery

class GeoQueryTest {
    @Test fun admitsKnownRegionAndRejectsContradictions() {
        assertEquals(GeoScope.MOSCOW, GeoScope.classify("Россия, г. Москва, Тверская, 12", null, null, null))
        assertEquals(GeoScope.OBLAST, GeoScope.classify("Химки, Ленина, 1", "Московская область", null, null))
        assertNull(GeoScope.classify("Тверь, Московская улица", null, null, null))
        assertNull(GeoScope.classify("Москва, Тверская", "Тверская область", null, null))
        assertNull(GeoScope.classify("Лесная улица", null, null, null))
    }

    @Test fun actualAgentQueryUsesDistrictOwnerPriceAndRooms() {
        val query = ListingQuery.read("Найди двушки от собственников в Даниловском районе до 15 миллионов")
        val offer = Offer(source="partner", region=GeoScope.MOSCOW, address="Москва, Павловская 1",
            district="Даниловский район", seller="owner", rooms=2, price=14_900_000, area=51.0)
        assertEquals(15_000_000L, query.budget)
        assertTrue(query.matches(offer))
        assertFalse(query.matches(offer.copy(price=15_100_000)))
        assertFalse(query.matches(offer.copy(seller="agent")))
        assertFalse(query.matches(offer.copy(rooms=1)))
        assertFalse(query.matches(offer.copy(district="Донской район")))
        assertFalse(query.matches(offer.copy(region="Тверская область")))
        assertFalse(query.matches(offer.copy(expiresAt=100)))
    }

    @Test fun multiselectCombinesWithStationAndRadius() {
        val query = GeoQuery(locations=listOf("Даниловский район","Химки"), stations=listOf("МЦК ЗИЛ","Тульская"),
            center=GeoScope.Point(55.709,37.625), radiusKm=2.0)
        val offer = Offer(source="csv", address="Москва, Лестева", district="Даниловский", transit="Тульская;Шаболовская",
            lat=55.712, lon=37.620)
        assertTrue(query.matches(offer))
        assertFalse(query.matches(offer.copy(transit="Тульская-2")))
        assertFalse(query.matches(offer.copy(lat=55.81)))
        assertFalse(query.matches(offer.copy(lat=null)))
    }
}
