package ru.vladis.realtorai.autopilot

import org.junit.Assert.*
import org.junit.Test
import ru.vladis.realtorai.data.*
import ru.vladis.realtorai.hunter.Offer

class AutoRulesTest {
    @Test fun canonicalTrackingOnly() {
        assertEquals("example.org/a?id=7",AutoRules.urlKey("https://www.example.org/a/?utm_source=x&id=7"))
        assertNotEquals(AutoRules.urlKey("https://example.org/a?id=7"),AutoRules.urlKey("https://example.org/a?id=8"))
        assertNull(AutoRules.urlKey("file:///test"))
    }
    @Test fun noWeakAddressIdentity() {
        assertNull(AutoRules.addressKey("Москва",45.0,2,5))
        assertNull(AutoRules.addressKey("Москва, ул. Примерная, 10",45.0,2,0))
    }
    @Test fun dedupAcrossSources() {
        val o=Offer(source="feed",url="https://www.example.org/item/7?utm_campaign=abc",price=5000000,area=45.0)
        val p=Property(title="Квартира",url="https://example.org/item/7",price=5100000)
        assertTrue(AutoRules.isDuplicate(o,p))
    }
    @Test fun skipRentalOrUnknown() {
        assertFalse(AutoRules.valid(Offer(source="feed",deal="rent",price=40000,area=45.0)))
        assertFalse(AutoRules.valid(Offer(source="feed",price=0,area=45.0)))
        assertTrue(AutoRules.valid(Offer(source="feed",price=5000000,area=45.0)))
    }
    @Test fun matchActiveBuyersOnly() {
        val c=Client(name="Клиент",budget=6000000)
        val props=listOf(Property(title="Да",price=5000000),Property(title="Дорого",price=7000000),Property(title="Архив",price=2000000,status="archived"),Property(title="Неизвестно"))
        assertEquals(listOf("Да"),AutoRules.matches(c,props).map{it.title})
        assertTrue(AutoRules.matches(c.copy(stage="lost"),props).isEmpty())
    }
}
