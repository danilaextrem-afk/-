package ru.vladis.realtorai.automation

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.vladis.realtorai.hunter.Offer
import ru.vladis.realtorai.hunter.ListingOrigin
import java.time.Instant
import java.util.concurrent.TimeUnit

class AuctionSource(private val channel: String, label: String) : ListingSource {
    override val name = "Торги.Открыто · $label"
    override suspend fun search(query: String): List<Offer> = withContext(Dispatchers.IO) {
        val url = "https://torgiopen.ru/api/lots".toHttpUrl().newBuilder()
            .addQueryParameter("category", "real_estate").addQueryParameter("kind", "Квартира")
            .addQueryParameter("channel", channel).addQueryParameter("sort", "fresh")
            .addQueryParameter("pageSize", "40").apply {
                if (query.isNotBlank()) addQueryParameter("q", query.trim())
            }.build()
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val body = checkNotNull(response.body)
            val items = JsonParser.parseReader(body.charStream()).asJsonObject.getAsJsonArray("items")
            items.mapNotNull { offer(it.asJsonObject) }
        }
    }

    internal fun offer(row: JsonObject): Offer? {
        fun JsonObject.value(key: String) = get(key)?.takeIf { it.isJsonPrimitive }?.asString
        val id = row.value("id") ?: return null
        val end = row.value("biddingEnd")?.let { runCatching { Instant.parse(it) }.getOrNull() }
        if (end != null && end <= Instant.now()) return null
        if (row.value("status") !in setOf("announced", "in_progress", "accepting_bids", "active", "accepting", "published")
            || row.value("currency") != "RUB" || row.value("transactionType") in setOf("rent", "lease")) return null
        val price = row.value("currentPrice") ?: row.value("startPrice")
        val region = row.get("region")?.takeIf { it.isJsonObject }?.asJsonObject?.value("name") ?: row.value("region")
        val images = row.get("photos")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { item ->
            when {
                item.isJsonPrimitive && item.asJsonPrimitive.isString -> item.asString
                item.isJsonObject -> item.asJsonObject.value("url")
                else -> null
            }
        }.orEmpty()
        val attrs = row.get("realEstateAttrs")?.takeIf { it.isJsonObject }?.asJsonObject
        return Offer(source = name, externalId = id, url = "https://torgiopen.ru/lot/$id",
            photoUrls = ListingOrigin.photos(images.joinToString("\n")).joinToString("\n").takeIf(String::isNotEmpty),
            sourceDescription = row.value("description")?.take(12_000), sourceUpdatedAt = ListingOrigin.updatedAt(row.value("updatedAt")),
            region = region, locality = row.value("city"), expiresAt = end?.toEpochMilli(), housingKind = attrs?.value("subcategory"), title = row.value("title") ?: return null, address = row.value("address").orEmpty(),
            price = price?.toDoubleOrNull()?.toLong() ?: return null,
            area = row.value("areaSqm")?.toDoubleOrNull() ?: return null,
            rooms = attrs?.value("rooms")?.toIntOrNull() ?: 0,
            floor = attrs?.value("floor")?.toIntOrNull() ?: 0,
            floorsTotal = attrs?.value("floors")?.toIntOrNull() ?: 0,
            lat = row.value("lat")?.toDoubleOrNull(), lon = row.value("lng")?.toDoubleOrNull())
    }

    companion object {
        private val http = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
    }
}
