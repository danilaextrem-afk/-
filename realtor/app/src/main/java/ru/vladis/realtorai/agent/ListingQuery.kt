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

internal data class ListingQuery(
    val place: String = "", val budget: Long = Long.MAX_VALUE,
    val rooms: Int? = null, val minArea: Double = 0.0,
    val market: String? = null, val seller: String? = null,
    val address: String = "", val station: String = ""
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
        return listOf("найди", "найти", "поищи", "покажи").any(text::contains) &&
            listOf("квартир", "двуш", "однуш", "треш", "четыреш", "новострой").any(text::contains)
    }

    suspend fun run(ctx: Context, db: AppDb, goal: String, args: JsonObject = JsonObject(), refresh: Boolean = false): String {
        val update = if (refresh) HuntRun.refreshAll(ctx) else null
        val query = ListingQuery.read(goal, args)
        val geography = GeoPreferences.read(ctx)
        val offers = db.hunter().allOffers()
        val candidates = offers.asSequence().filter { query.matches(it) && geography.matches(it) }.sortedBy { it.price }
        val selected = mutableListOf<Offer>()
        for (offer in candidates) {
            if (selected.none { it.url.isNotBlank() && it.url == offer.url || Dedup.same(it, offer) }) selected += offer
            if (selected.size >= 8) break
        }
        val answer = if (selected.isNotEmpty()) selected.joinToString("\n\n") {
            "${it.title} · ${it.price.rub()} · ${it.address} · ${it.source}\n${it.url}"
        } else if (query.seller != null) {
            "В подключённых источниках нет совпадений с подтверждённым типом продавца. Для обычных объявлений подключите фид с полем продавца."
        } else GeoScope.searchableProperties(db.dao().allProperties(), offers, geography).asSequence().filter { property ->
            property.status == "active" && query.matches(Offer(source = "CRM", address = property.address,
                region = property.region, price = property.price, area = property.area, rooms = property.rooms, market = property.type))
        }.sortedBy { it.price }.take(8).joinToString("\n") { "#${it.id} ${it.title} · ${it.price.rub()} · ${it.address}" }
            .ifBlank { "По этим параметрам нет подтверждённых объектов в Москве и МО." }
        return if (update != null && update.errors.isNotEmpty()) answer + "\n\nЧасть источников не ответила: " + update.errors.take(3).joinToString(" · ")
        else answer
    }
}
