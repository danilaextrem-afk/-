package ru.vladis.realtorai.owner

import org.junit.Assert.*
import org.junit.Test
import ru.vladis.realtorai.data.Property
import ru.vladis.realtorai.hunter.Offer
import java.time.ZoneId

class OwnerRulesTest {
    @Test fun considersOnlyDistinctFreshComparableSalesAndUsesCheapestFive() {
        val now = 100 * OwnerRules.day
        val property = Property(title = "Квартира", address = "Москва, Лесная 1", housingKind = "apartment", rooms = 2, area = 50.0)
        fun offer(i: Int) = Offer(source = "feed", url = "https://example.org/ad/$i", district = "Тверской",
            address = "Москва, Цветной бульвар ${20+i*3}", housingKind = "apartment", rooms = 2, area = 50.0,
            price = 5_000_000L + i * 100_000L, lastSeen = now)
        val input = (0..6).map(::offer) + listOf(
            offer(99).copy(price = 1, deal = "rent"), offer(98).copy(price = 1, rooms = 3),
            offer(97).copy(price = 1, lastSeen = 0), offer(0).copy(url = "https://example.org/duplicate"),
            offer(96).copy(price = 1, address = property.address)
        )
        val comps = OwnerRules.comparisons(property, input, "Тверской", now)
        assertEquals((0..4).map { offer(it).price }, comps.map { it.price })
        assertEquals(listOf(4_940_000L, 5_200_000L, 5_460_000L), OwnerRules.estimate(50.0, comps, PriceStrategy()))
        assertTrue(OwnerRules.estimate(50.0, comps.take(2), PriceStrategy()).isEmpty())
    }

    @Test fun metricsStagesAndReportIntervals() {
        assertEquals("нет данных", OwnerRules.metric(null))
        assertEquals("0", OwnerRules.metric(0))
        assertEquals(OwnerStep.AGREEMENT, OwnerRules.next(OwnerStep.CALL))
        assertEquals(OwnerStep.FINISHED, OwnerRules.next(OwnerStep.FINISHED))
        assertEquals(7 * OwnerRules.day, OwnerRules.reportDue(0, 1, ZoneId.of("UTC")))
        assertEquals(4 * OwnerRules.day, OwnerRules.reportDue(0, 2, ZoneId.of("UTC")))
    }
}
