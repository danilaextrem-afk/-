package ru.vladis.realtorai.hunter

import org.junit.Assert.*
import org.junit.Test

class HunterLogicTest {
    private fun o(id: Long, price: Long = 10_000_000, area: Double = 50.0, rooms: Int = 2, floor: Int = 3, addr: String = "Москва, ул. Ленина, д. $id",
                  deal: String = "sale", district: String = "") =
        Offer(id = id, source = "t", price = price, area = area, rooms = rooms, floor = floor, address = addr, deal = deal, district = district)

    @Test fun estimateNeedsFiveComps() {
        val comps = (1L..4L).map { o(it, price = 10_000_000) }
        val target = o(100, price = 7_500_000, addr = "другой адрес 100")
        assertNull(Market.estimate(target, comps + target, Dedup.clusters(comps + target)))
    }

    @Test fun estimateAndDiscount() {
        val comps = (1L..6L).map { o(it, price = 10_000_000, addr = "улица $it") }  // 200 000 ₽/м²
        val t = o(100, price = 7_500_000, addr = "проспект 100")
        val all = comps + t; val e = Market.estimate(t, all, Dedup.clusters(all))!!
        assertEquals(10_000_000L, e.value); assertEquals(6, e.comps)
        assertEquals(0.25, Market.discount(t, e), 1e-9)
    }

    @Test fun duplicatesAreGroupedAndCountedOnceAsComps() {
        val a = o(1, addr = "Москва, ул. Ленина, д. 5"); val b = o(2, area = 50.5, addr = "г. Москва, Ленина 5")
        val c = o(3, floor = 7, addr = "Москва, ул. Ленина, д. 5")
        val cl = Dedup.clusters(listOf(a, b, c))
        assertEquals(cl[1], cl[2]); assertNotEquals(cl[1], cl[3])
    }

    @Test fun differentRoomsNotDuplicate() = assertFalse(Dedup.same(o(1, rooms = 2, addr = "Ленина 5"), o(2, rooms = 3, addr = "Ленина 5")))

    @Test fun investCalc() {
        val r = Invest.calc(Invest.In(10_000_000, 50_000, vacancyPct = 10.0, costsYear = 60_000, taxPct = 13.0))
        assertEquals(0.06, r.grossYield, 1e-9); assertEquals(417_600L, r.netAnnual)
        assertEquals(0.04176, r.netYield, 1e-9); assertEquals(10_000_000 / 417_600.0, r.paybackYears!!, 1e-6)
    }

    @Test fun investNoPaybackWhenLoss() = assertNull(Invest.calc(Invest.In(1_000_000, 1_000, costsYear = 100_000)).paybackYears)

    @Test fun commission() = assertEquals(120_000.0, Commission.agent(10_000_000, 3.0, 40.0), 1e-6)

    @Test fun csvSemicolonQuotes() {
        val offers = HunterImport.fromCsv("Цена;Площадь;Комнат;Этаж;Адрес;Сделка\n\"12 500 000\";\"54,3\";2;5/9;\"Москва, Тверская, 1\";продажа\n", "csv")
        assertEquals(1, offers.size); val x = offers[0]
        assertEquals(12_500_000L, x.price); assertEquals(54.3, x.area, 1e-9); assertEquals(5, x.floor); assertEquals(9, x.floorsTotal); assertEquals("sale", x.deal)
    }

    @Test fun jsonImportAndRent() {
        val offers = HunterImport.fromJson("""{"offers":[{"price":80000,"area":40,"rooms":1,"deal":"аренда","address":"X 1"}]}""", "j")
        assertEquals("rent", offers[0].deal); assertEquals(80_000L, offers[0].price)
    }

    @Test fun planDetectsNewChangedAndSkipped() {
        val existing = listOf(o(1, price = 100).copy(externalId = "A"))
        val rows = listOf(o(0, price = 90).copy(externalId = "A"), o(0, price = 50, addr = "новый 7").copy(externalId = "B"), o(0, price = 0).copy(externalId = "C"))
        val p = HunterImport.plan(existing, rows, 1L)
        assertEquals(1, p.inserts.size); assertEquals(1, p.updates.size); assertTrue(p.updates[0].second); assertEquals(1, p.skipped)
        assertEquals(90L, p.updates[0].first.price)
    }

    @Test fun planUnchangedPrice() {
        val p = HunterImport.plan(listOf(o(1).copy(externalId = "A")), listOf(o(0).copy(externalId = "A")), 5L)
        assertFalse(p.updates[0].second)
    }

    @Test fun filterRooms() {
        val f = SavedFilter(rooms = "4")
        assertTrue(Filters.matches(o(1, rooms = 5), f)); assertFalse(Filters.matches(o(2, rooms = 2), f))
        assertFalse(Filters.matches(o(3, deal = "rent"), SavedFilter()))
    }

    @Test fun alertsWithoutFiltersOnlyBelowMarket() {
        val comps = (1L..6L).map { o(it, addr = "улица $it") }; val cheap = o(100, price = 7_000_000, addr = "а 100"); val fair = o(101, addr = "б 101")
        val all = comps + cheap + fair; val hits = Alerts.select(listOf(cheap, fair), all, Dedup.clusters(all), emptyList())
        assertEquals(listOf(100L), hits.map { it.first.id })
    }

    @Test fun prefilterRespectsBudgetAndLimit() {
        val deals = (1L..20L).map { Deal(o(it, price = it * 1_000_000), null, null, 1, true, 0) }
        val r = Facts.prefilter(10_000_000, deals, max = 5)
        assertEquals(5, r.size); assertTrue(r.all { it.o.price <= 10_500_000 })
    }
}
