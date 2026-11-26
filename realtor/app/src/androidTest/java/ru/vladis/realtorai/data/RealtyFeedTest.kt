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
}
