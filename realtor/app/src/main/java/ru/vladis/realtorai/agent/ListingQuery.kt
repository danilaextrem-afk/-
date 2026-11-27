package ru.vladis.realtorai.agent

import android.content.Context
import com.google.gson.JsonObject
import ru.vladis.realtorai.data.AppDb
import ru.vladis.realtorai.hunter.Dedup
import ru.vladis.realtorai.hunter.GeoPreferences
import ru.vladis.realtorai.hunter.HuntRun
import ru.vladis.realtorai.ui.rub
import ru.vladis.realtorai.hunter.GeoScope
import ru.vladis.realtorai.hunter.Offer
import ru.vladis.realtorai.hunter.ListingOrigin

internal data class ListingQuery(
    val place: String = "", val budget: Long = Long.MAX_VALUE,
    val rooms: Int? = null, val minArea: Double = 0.0,
    val market: String? = null, val seller: String? = null,
    val address: String = "", val station: String = "", val cheapestFirst: Boolean = false
) {
    fun matches(offer: Offer): Boolean {
        return GeoScope.eligible(offer) && offer.deal == "sale" && offer.price in 1..budget &&
            offer.area >= minArea && (rooms == null || offer.rooms == rooms) &&
            (market == null || offer.market == market) && (seller == null || offer.seller == seller) &&
            GeoScope.within(place, offer) &&
            GeoScope.addressMatches(offer.address, address) &&
            (station.isBlank() || ru.vladis.realtorai.hunter.GeoQuery(stations = listOf(station)).matches(offer))
    }

    companion object {
        private val amount = Regex("\\bдо\\s+([0-9]+(?:[.,][0-9]+)?(?:[ \u00a0][0-9]{3})*)(?:\\s*(млн|миллион\\p{L}*|тыс\\.?|тысяч\\p{L}*))?")
        private val district = Regex("\\b(?:в|по)\\s+([\\p{L}\\s-]+?)\\s+(?:районе|району|округе)\\b")
        private val roomsCount = Regex("\\b([1-9])\\s*[- ]?\\s*(?:комн\\p{L}*|к\\b)")
        fun read(goal: String, args: JsonObject = JsonObject()): ListingQuery {
            val text = GeoScope.normalize(goal)
            fun arg(key: String) = args.get(key)?.asString.orEmpty().trim().take(160)
            val price = amount.find(text)?.let {
                val value = it.groupValues[1].replace(" ", "").replace("\u00a0", "").replace(',', '.').toDoubleOrNull()
                val scale = when {
                    it.groupValues[2].startsWith("м") -> 1_000_000
                    it.groupValues[2].startsWith("т") -> 1_000
                    else -> 1
                }
                value?.let { n -> (n * scale).toLong().takeIf { it in 1..1_000_000_000_000 } }
            }
            val rooms = roomsCount.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: when {
                "однуш" in text -> 1
                "двуш" in text -> 2
                "треш" in text -> 3
                "четыреш" in text -> 4
                else -> arg("rooms").toIntOrNull()?.takeIf { it in 0..20 }
            }
            val place = district.find(text)?.groupValues?.get(1) ?: arg("area").ifBlank {
                when {
                    Regex("нов[аояй]+ моск").containsMatchIn(text) || "тинао" in text -> "Новая Москва"
                    "подмосков" in text || "московской област" in text -> "МО"
                    "в москве" in text -> "Москва"
                    else -> ""
                }
            }
            val market = when {
                "новострой" in text || "первич" in text || arg("market") == "primary" -> "primary"
                "вторич" in text || arg("market") == "secondary" -> "secondary"
                else -> null
            }
            val seller = when {
                "от собственник" in text || "от хозя" in text || arg("seller") == "owner" -> "owner"
                "от застройщик" in text || arg("seller") == "developer" -> "developer"
                else -> null
            }
            return ListingQuery(place, price ?: arg("budget").toLongOrNull()?.takeIf { it > 0 } ?: Long.MAX_VALUE,
                rooms, arg("minArea").toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } ?: 0.0,
                market, seller, arg("address"), arg("station"))
        }
    }
}


internal object ListingSearch {
    fun accepts(goal: String): Boolean {
        val text = GeoScope.normalize(goal)
        return listOf("найди", "найти", "поищи", "покажи", "подбери", "ищу", "хочу купить").any(text::contains) &&
            listOf("квартир", "жиль", "двуш", "однуш", "треш", "четыреш", "новострой", "вариант").any(text::contains)
    }

    suspend fun run(ctx: Context, db: AppDb, goal: String, args: JsonObject = JsonObject(), refresh: Boolean = false): String {
        return results(ctx, db, ListingQuery.read(goal, args), refresh).text
    }

    suspend fun results(
        ctx: Context, db: AppDb, query: ListingQuery, refresh: Boolean, seen: Set<String> = emptySet()
    ): ListingReply {
        // Ordinary apartments arrive from configured feeds. The auctions connector is not a substitute.
        val update = if (refresh) HuntRun.refreshAll(ctx, includeCatalog = false) else null
        val geography = GeoPreferences.read(ctx)
        val offers = db.hunter().allOffers()
        fun identity(offer: Offer): String? = ListingOrigin.original(offer)?.identity
        val previouslyShown = offers.filter { identity(it) in seen || "record:${it.id}" in seen }
        val order = compareByDescending<Offer> { ListingOrigin.ownerApartment(it) }
        val ranked = if (query.cheapestFirst) order.thenBy { it.price }.thenByDescending { it.sourceUpdatedAt ?: it.lastSeen }
            else order.thenByDescending { it.sourceUpdatedAt ?: it.lastSeen }.thenBy { it.price }
        val candidates = offers.asSequence().filter {
            query.matches(it) && geography.matches(it) && identity(it) != null &&
                !it.source.startsWith("Торги.Открыто") &&
                GeoScope.normalize(it.housingKind.orEmpty()) in setOf("", "квартира", "квартиры", "flat", "apartment")
        }.sortedWith(ranked)
        val selected = mutableListOf<Offer>()
        for (offer in candidates) {
            val key = identity(offer)
            if (key in seen || "record:${offer.id}" in seen) continue
            if (previouslyShown.any { Dedup.same(it, offer) }) continue
            if (selected.none { identity(it) == key || Dedup.same(it, offer) }) selected += offer
            if (selected.size >= 8) break
        }
        val answer = when {
            selected.isNotEmpty() -> selected.joinToString("\n\n") {
                val original = requireNotNull(ListingOrigin.original(it))
                "${it.title} · ${it.price.rub()} · ${it.address} · ${it.source} · ID ${original.id}\n${original.url}"
            }
            seen.isNotEmpty() -> "В подключённых источниках больше нет новых квартир по этим условиям."
            db.hunter().allSubs().isEmpty() && offers.none { ListingOrigin.original(it) != null && !it.source.startsWith("Торги.Открыто") } ->
                "Подключите разрешённый фид квартир Москвы и области в разделе «Поиск → Данные» или импортируйте файл объявлений."
            else -> "В подключённых источниках нет квартир с прямой ссылкой по этим условиям."
        }
        val report = if (update != null && update.errors.isNotEmpty()) {
            answer + "\n\nЧасть источников не ответила: " + update.errors.take(3).joinToString(" · ")
        } else answer
        return ListingReply(report, selected.map { it.id }, selected.flatMap { listOfNotNull(identity(it), "record:${it.id}") },
            selected.minOfOrNull { it.price })
    }
}
