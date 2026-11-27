package ru.vladis.realtorai.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.hunter.RealtyFeed

@RunWith(AndroidJUnit4::class)
class RealtyFeedTest {
    @Test fun readsDeveloperYrlNamespaceAndFiltersForeignCurrency() {
        val offer = """
            <offer internal-id="A2"><type>продажа</type><category>квартира</category><new-flat>1</new-flat>
            <location><locality-name>Москва</locality-name><address>Лесная, 4</address><sub-locality-name>Тверской</sub-locality-name></location>
            <price><value>13000000</value><currency>RUB</currency></price><area><value>52.1</value></area><rooms>2</rooms></offer>
        """.trimIndent()
        val xml = """<realty-feed xmlns="http://webmaster.yandex.ru/schemas/feed/realty/2010-06">$offer${offer.replace("RUB","EUR")}</realty-feed>"""
        val current = RealtyFeed.parse(xml, "feed:test").single()
        assertEquals("A2", current.externalId)
        assertEquals("primary", current.market)
        assertEquals("Тверской", current.district)
        assertEquals(13_000_000L, current.price)
    }

    @Test fun preservesRepeatedPhotosAndCallsOnlyExplicitOwnerNumbers() {
        val xml = """
            <realty-feed><offer internal-id="feed-781"><type>продажа</type><category>квартира</category>
              <url>https://www.cian.ru/sale/flat/12345678/</url>
              <last-update-date>2026-11-26T09:00:00+03:00</last-update-date>
              <image>https://cdn.example.org/one.jpg</image><image>https://cdn.example.org/two.jpg</image>
              <location><locality-name>Москва</locality-name><address>Лесная, 4</address></location>
              <price><value>13000000</value><currency>RUB</currency></price><area><value>52.1</value></area>
              <sales-agent><category>владелец</category><phone>+7 999 123-45-…</phone><phone>+7 999 123-45-67</phone></sales-agent>
            </offer></realty-feed>
        """.trimIndent()
        val row = RealtyFeed.parse(xml, "allowed-test").single()
        val origin = ru.vladis.realtorai.hunter.ListingOrigin
        assertEquals(2, origin.photos(row.photoUrls).size)
        assertEquals("12345678", origin.original(row)?.id)
        assertEquals("+79991234567", row.contactPhone)
        assertNull(row.ownerPhone)
        assertNotNull(row.sourceUpdatedAt)
    }
}
