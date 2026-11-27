package ru.vladis.realtorai.agent

import org.junit.Assert.*
import org.junit.Test

class PropertyIndexTest {
    @Test fun acceptsDirectCardsAndStripsTracking() {
        val cian = PropertyIndex.sites.single { it.name == "Циан" }
        assertEquals("https://cian.ru/sale/flat/336656970/",
            PropertyIndex.canonical(cian, "https://www.cian.ru/sale/flat/336656970/?utm_source=test#view"))
        assertNull(PropertyIndex.canonical(cian, "https://www.cian.ru/kupit-kvartiru/"))
        assertNull(PropertyIndex.canonical(cian, "https://www.cian.ru/rent/flat/336656970/"))
        assertNull(PropertyIndex.canonical(cian, "https://cian.ru.example.com/sale/flat/336656970/"))
        assertNull(PropertyIndex.canonical(cian, "https://user@www.cian.ru/sale/flat/336656970/"))
    }

    @Test fun continuationKeepsThePriorQuery() {
        val query = ListingQuery(place = "Химки", rooms = 2)
        val prior = SearchConversation(current = query, seen = listOf("index:https://example.com/1"))
        assertEquals(SearchTurn("refine", query), SearchLanguage.offline("Найди ещё", prior))
    }
}
