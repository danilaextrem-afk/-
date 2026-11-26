package ru.vladis.realtorai.hunter

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Чистая логика без Android-зависимостей (покрыта unit-тестами). Всё считается ТОЛЬКО по импортированным данным. */
object Market {
    const val MIN_COMPS = 5
    const val DEAL_THRESHOLD = 0.10
    data class Estimate(val value: Long, val low: Long, val high: Long, val medianPpsm: Long, val comps: Int)

    fun ppsm(o: Offer) = if (o.area > 0) o.price / o.area else 0.0

    /** Аналоги: тот же тип сделки (для продажи — и рынок), комнаты, площадь ±25%, один представитель на группу дублей.
     *  Если в районе объекта аналогов ≥ MIN_COMPS — берутся только они. */
    fun comps(o: Offer, all: List<Offer>, clusters: Map<Long, Long>, deal: String = o.deal): List<Offer> {
        val me = clusters[o.id] ?: o.id
        val base = all.filter {
            it.id != o.id && it.active && it.deal == deal && it.price > 0 && it.area > 0 &&
                (deal != "sale" || it.market == o.market) &&
                (o.rooms == 0 || it.rooms == o.rooms) && (o.area <= 0 || abs(it.area - o.area) <= 0.25 * o.area) &&
                (clusters[it.id] ?: it.id) != me
        }
        val one = base.groupBy { clusters[it.id] ?: it.id }.map { g -> g.value.minBy { it.id } }
        val local = if (o.district.isNotBlank()) one.filter { it.district.equals(o.district, true) } else emptyList()
        return if (local.size >= MIN_COMPS) local else one
    }

    fun pct(s: List<Double>, q: Double): Double {
        val pos = q * (s.size - 1); val lo = pos.toInt(); val hi = minOf(lo + 1, s.size - 1)
        return s[lo] + (s[hi] - s[lo]) * (pos - lo)
    }

    /** null — аналогов меньше MIN_COMPS: оценка НЕ даётся. */
    fun estimate(o: Offer, all: List<Offer>, clusters: Map<Long, Long>): Estimate? {
        if (o.area <= 0 || o.deal != "sale") return null
        val c = comps(o, all, clusters); if (c.size < MIN_COMPS) return null
        val p = c.map { ppsm(it) }.sorted()
        return Estimate((pct(p, .5) * o.area).toLong(), (pct(p, .25) * o.area).toLong(), (pct(p, .75) * o.area).toLong(), pct(p, .5).toLong(), c.size)
    }
    fun discount(o: Offer, e: Estimate) = if (e.value > 0) (e.value - o.price).toDouble() / e.value else 0.0
}

object Dedup {
    private val stop = setOf("г", "город", "москва", "ул", "улица", "д", "дом", "пр", "проспект", "пер", "переулок", "корп", "корпус", "к", "стр", "строение", "россия", "московская", "область")
    fun norm(a: String) = a.lowercase().replace('ё', 'е').split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() && it !in stop }.joinToString(" ")
    fun fp(o: Offer) = norm(o.address) + "|" + o.rooms + "|" + o.floor + "|" + o.area.roundToInt()

    private fun jaccard(a: String, b: String): Double {
        val x = a.split(' ').toSet(); val y = b.split(' ').toSet()
        return x.intersect(y).size.toDouble() / x.union(y).size
    }
    /** Один и тот же объект: тот же тип сделки, комнаты, этаж, площадь (±1 м² или 2%) и похожий адрес. */
    fun same(a: Offer, b: Offer): Boolean {
        if (a.deal != b.deal || a.area <= 0 || b.area <= 0) return false
        if (a.rooms > 0 && b.rooms > 0 && a.rooms != b.rooms) return false
        if (abs(a.area - b.area) > max(1.0, 0.02 * a.area)) return false
        if (a.floor > 0 && b.floor > 0 && a.floor != b.floor) return false
        val na = norm(a.address); val nb = norm(b.address)
        return na.isNotBlank() && nb.isNotBlank() && jaccard(na, nb) >= 0.8
    }
    /** id объявления -> id группы (минимальный id в группе). */
    fun clusters(list: List<Offer>): Map<Long, Long> {
        val parent = HashMap<Long, Long>().apply { list.forEach { put(it.id, it.id) } }
        fun find(x: Long): Long { var r = x; while (parent[r] != r) r = parent[r]!!; parent[x] = r; return r }
        list.groupBy { it.deal to it.rooms }.values.forEach { g ->
            for (i in g.indices) for (j in i + 1 until g.size) if (same(g[i], g[j])) {
                val a = find(g[i].id); val b = find(g[j].id); if (a != b) parent[max(a, b)] = minOf(a, b)
            }
        }
        return list.associate { it.id to find(it.id) }
    }
}

object Invest {
    data class In(val price: Long, val monthlyRent: Long, val vacancyPct: Double = 0.0, val costsYear: Long = 0, val taxPct: Double = 0.0)
    data class Out(val grossYield: Double, val netYield: Double, val netAnnual: Long, val paybackYears: Double?)

    /** Параметры (простой, расходы, налог) вводит пользователь; по умолчанию 0 — ничего не предполагается. */
    fun calc(i: In): Out {
        val gross = i.monthlyRent * 12.0
        val afterCosts = gross * (1 - i.vacancyPct / 100) - i.costsYear
        val net = afterCosts - max(0.0, afterCosts) * i.taxPct / 100
        return Out(if (i.price > 0) gross / i.price else 0.0, if (i.price > 0) net / i.price else 0.0, net.toLong(), if (net > 0) i.price / net else null)
    }
    /** Оценка аренды по аналогам аренды в базе: (руб/мес, число аналогов); null если < MIN_COMPS. */
    fun rentEstimate(o: Offer, all: List<Offer>, clusters: Map<Long, Long>): Pair<Long, Int>? {
        if (o.area <= 0) return null
        val c = Market.comps(o, all, clusters, "rent"); if (c.size < Market.MIN_COMPS) return null
        return (Market.pct(c.map { Market.ppsm(it) }.sorted(), .5) * o.area).toLong() to c.size
    }
}

object Commission { fun agent(price: Long, agencyPct: Double, sharePct: Double) = price * agencyPct / 100 * sharePct / 100 }

object Filters {
    fun matches(o: Offer, f: SavedFilter): Boolean {
        if (o.deal != "sale") return false
        if (f.maxPrice > 0 && o.price > f.maxPrice) return false
        if (f.minArea > 0 && o.area < f.minArea) return false
        val rs = f.rooms.split(',').mapNotNull { it.trim().toIntOrNull() }
        if (rs.isNotEmpty() && !rs.any { r -> if (r >= 4) o.rooms >= 4 else o.rooms == r }) return false
        val d = f.district.trim()
        return d.isBlank() || o.district.contains(d, true) || o.address.contains(d, true)
    }
}

data class Deal(val o: Offer, val est: Market.Estimate?, val discount: Double?, val dups: Int, val repr: Boolean, val delta: Long) {
    val below get() = discount != null && discount >= Market.DEAL_THRESHOLD
    companion object {
        fun build(offers: List<Offer>, pts: List<PricePoint>): List<Deal> {
            val cl = Dedup.clusters(offers); val size = cl.values.groupingBy { it }.eachCount(); val byO = pts.groupBy { it.offerId }
            return offers.map { o ->
                val e = Market.estimate(o, offers, cl); val p = byO[o.id].orEmpty().sortedBy { it.at }
                Deal(o, e, e?.let { Market.discount(o, it) }, size[cl[o.id]] ?: 1, (cl[o.id] ?: o.id) == o.id,
                    if (p.size >= 2) p.last().price - p[p.size - 2].price else 0)
            }
        }
    }
}

object Alerts {
    /** Кого показывать в уведомлении: совпавшие с включёнными фильтрами; без фильтров — только дешевле рынка. */
    fun select(cand: List<Offer>, all: List<Offer>, clusters: Map<Long, Long>, filters: List<SavedFilter>): List<Pair<Offer, Double?>> {
        val act = filters.filter { it.notify }
        return cand.mapNotNull { o ->
            val e = Market.estimate(o, all, clusters); val d = e?.let { Market.discount(o, it) }
            val below = d != null && d >= Market.DEAL_THRESHOLD
            val ok = if (act.isEmpty()) o.deal == "sale" && below else act.any { Filters.matches(o, it) && (!it.onlyBelowMarket || below) }
            if (ok) o to d else null
        }
    }
}

object Facts {
    /** Факты для переговоров — только то, что реально есть в данных. */
    fun negotiation(d: Deal, history: List<PricePoint>, nowMs: Long): List<String> {
        val o = d.o; val f = mutableListOf("Цена ${o.price} ₽, ${Market.ppsm(o).toLong()} ₽/м², ${o.area} м², ${o.rooms} комн.")
        d.est?.let { f += "Оценка по ${it.comps} аналогам из базы: медиана ${it.medianPpsm} ₽/м², справедливая цена ≈ ${it.value} ₽ (диапазон ${it.low}–${it.high}); цена объекта ${if (o.price > it.value) "выше" else "ниже"} оценки на ${abs((Market.discount(o, it) * 100).roundToInt())}%" }
            ?: run { f += "Оценка рынка недоступна: аналогов меньше ${Market.MIN_COMPS}" }
        val h = history.sortedBy { it.at }
        if (h.size >= 2) f += "История цены в базе: " + h.joinToString(" → ") { it.price.toString() }
        f += "В базе с ${((nowMs - o.firstSeen) / 86_400_000)} дн. назад (дата первого появления в наших данных, не дата публикации)"
        if (d.dups > 1) f += "Объект встречается в ${d.dups} объявлениях (дубли/несколько агентств)"
        return f
    }
    /** Предварительный отбор для ИИ-подбора: бюджет (+5%) и сортировка по выгодности; в Groq уходит не больше max строк. */
    fun prefilter(budget: Long, deals: List<Deal>, max: Int = 15): List<Deal> =
        deals.filter { it.o.deal == "sale" && it.o.active && it.repr && (budget <= 0 || it.o.price <= budget * 1.05) }
            .sortedWith(compareByDescending<Deal> { it.discount ?: -1.0 }.thenBy { Market.ppsm(it.o) }).take(max)
}
