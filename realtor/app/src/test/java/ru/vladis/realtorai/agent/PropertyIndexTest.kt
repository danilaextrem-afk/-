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

    @Test fun acceptsBothDirectMoveRoutesAsOneListing() {
        val move = PropertyIndex.sites.single { it.domain == "move.ru" }
        val short = "https://move.ru/objects/9293358415"
        val long = "https://move.ru/objects/prodaetsya_1-komnatnaya_kvartira_ploschadyu_313_kvm_moskva_9293358415/"
        assertEquals(short, PropertyIndex.canonical(move, "$short?utm_source=test"))
        assertEquals(long, PropertyIndex.canonical(move, long))
        val shortId = ru.vladis.realtorai.publish.SaleIntake.sharedLink(short)?.identity
        assertEquals("move.ru:9293358415", shortId)
        assertEquals(shortId, ru.vladis.realtorai.publish.SaleIntake.sharedLink(long)?.identity)
        assertNull(PropertyIndex.canonical(move, "https://move.ru/objects/"))
        assertNull(PropertyIndex.canonical(move, "https://move.ru/objects/sdaetsya_kvartira_9293358415/"))
    }

    @Test fun continuationKeepsThePriorQuery() {
        val query = ListingQuery(place = "Химки", rooms = 2)
        val prior = SearchConversation(current = query, seen = listOf("index:https://example.com/1"))
        assertEquals(SearchTurn("refine", query), SearchLanguage.offline("Найди ещё", prior))
    }
    @Test fun keepsLaterPlatformsVisibleWithinTheDisplayLimit() {
        val first = (1..10).map { "first-$it" }
        val second = (1..10).map { "second-$it" }
        assertEquals(listOf("first-1", "second-1", "cian", "avito"),
            PropertyIndex.roundRobin(listOf(first, second, listOf("cian"), emptyList(), listOf("avito"))).take(4))
    }

    @Test fun retriesBurstLimitButNotExhaustedMonthlyQuota() {
        assertEquals(1L, PropertyIndex.burstRetrySeconds("0,50", "1,998232", null))
        assertNull(PropertyIndex.burstRetrySeconds("0,0", "1,998232", null))
        assertNull(PropertyIndex.burstRetrySeconds("0,50", "1,998232", "120"))
    }

    @Test fun indexedPublicPostCanOpenTheSameSaleReviewAsTheSharedLink() {
        val telegram = PropertyIndex.sites.single { it.domain == "t.me" }
        val indexed = requireNotNull(PropertyIndex.canonical(telegram, "https://t.me/s/building_news/47"))
        val shared = ru.vladis.realtorai.publish.SaleIntake.sharedLink("https://t.me/building_news/47")
        assertEquals(shared?.identity, ru.vladis.realtorai.publish.SaleIntake.sharedLink(indexed)?.identity)
        assertNotNull(shared)
        assertNull(ru.vladis.realtorai.publish.SaleIntake.sharedLink("https://www.cian.ru/rent/flat/336656970/"))
    }
}
