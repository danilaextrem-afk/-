package ru.vladis.realtorai.agent

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.os.SystemClock
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.vladis.realtorai.data.Store
import ru.vladis.realtorai.hunter.ListingOrigin
import ru.vladis.realtorai.hunter.Offer
import java.util.concurrent.TimeUnit

internal data class IndexedListing(val source: String, val title: String, val url: String)
internal data class SearchSourceStatus(val source: String, val count: Int, val status: String)
internal data class IndexResult(val pages: List<IndexedListing>, val sources: List<SearchSourceStatus>)

internal object PropertyIndex {
    internal data class Site(val name: String, val domain: String, val path: Regex)
    internal val sites = listOf(
        Site("Авито", "avito.ru", Regex("/[^/]+/kvartiry/[^/]+_[0-9]+/?")),
        Site("Циан", "cian.ru", Regex("/sale/flat/[0-9]+/?")),
        Site("Домклик", "domclick.ru", Regex("/card/sale/[0-9a-fA-F-]{36}/?")),
        Site("Яндекс Недвижимость", "realty.yandex.ru", Regex("/offer/[0-9]+/?")),
        Site("М2", "m2.ru", Regex("/[^/]+/nedvizhimost/[^/]+/(?:[0-9a-fA-F-]{36}|[^/]+_[0-9]{5,})/?")),
        Site("ONREALT", "onrealt.ru", Regex("/[^/]+/k[uy]pit-kvartiru(?:-[^/]+)?/[0-9]+/?")),
        Site("Мир квартир", "mirkvartir.ru", Regex("/[0-9]{5,}/?")),
        Site("ГдеЭтотДом", "gdeetotdom.ru", Regex("/obj/living/(?:[^/]+/)?[0-9]+/?")),
        Site("Move.ru", "move.ru", Regex("/objects/prodaetsya_[^/]+_[0-9]+/?")),
        Site("Из рук в руки", "irr.ru", Regex("/real-estate/apartments-sale/.+-advert[0-9]+[.]html")),
        Site("ПИК · застройщик", "pik.ru", Regex("/flat/[0-9]+/?")),
        Site("Telegram · публичные сообщения", "t.me", Regex("/(?:s/)?[A-Za-z][A-Za-z0-9_]{3,}/[0-9]+/?")),
        Site("ВКонтакте · публичные записи", "vk.com", Regex("/wall-?[0-9]+_[0-9]+/?"))
    )
    private val http = OkHttpClient.Builder().followRedirects(false)
        .callTimeout(9, TimeUnit.SECONDS).build()
    private val requestPace = Mutex()
    private var nextRequestAt = 0L

    private suspend fun waitForSlot() = requestPace.withLock {
        delay((nextRequestAt - SystemClock.elapsedRealtime()).coerceAtLeast(0))
        nextRequestAt = SystemClock.elapsedRealtime() + 1_100
    }

    internal fun burstRetrySeconds(remaining: String?, reset: String?, retryAfter: String?): Long? {
        // Brave sends the burst window first and the monthly window second.
        val quota = remaining?.split(',')?.mapNotNull { it.trim().toLongOrNull() }.orEmpty()
        if (quota.size > 1 && quota.last() == 0L) return null
        val seconds = retryAfter?.toLongOrNull() ?: reset?.substringBefore(',')?.trim()?.toLongOrNull() ?: 1L
        return seconds.takeIf { it in 0..5 }?.coerceAtLeast(1)
    }

    internal fun canonical(site: Site, raw: String): String? {
        val url = ListingOrigin.webUrl(raw) ?: return null
        if (url.host != site.domain && !url.host.endsWith(".${site.domain}")) return null
        if (!site.path.matches(url.encodedPath)) return null
        return url.newBuilder().host(url.host.removePrefix("www.")).query(null).fragment(null).build().toString()
    }

    private fun phrase(q: ListingQuery) = buildString {
        append("купить квартиру ").append(q.place.ifBlank { """(Москва OR "Московская область")""" })
        q.rooms?.let { append(if (it == 0) " студия" else " $it-комнатная") }
        if (q.budget < Long.MAX_VALUE) append(" до ").append(q.budget).append(" руб")
        if (q.minArea > 0) append(" от ").append(q.minArea).append(" м²")
        if (q.market != null) append(if (q.market == "primary") " новостройка" else " вторичка")
        if (q.seller != null) append(when(q.seller) { "owner" -> " от собственника"; "developer" -> " от застройщика"; else -> " от агентства" })
        if (q.insideMkad) append(" внутри МКАД")
        q.metroWalk?.let { append(" метро до ").append(it).append(" минут пешком") }
        append(" ").append(q.address).append(" ").append(q.station)
    }.take(430).replace(Regex("(?i)(?:site|filetype|inurl):\\S+"), "")

    suspend fun search(ctx: Context, q: ListingQuery, page: Int, seen: Set<String>): IndexResult = coroutineScope {
        val key = Store(ctx).propertyIndexKey
        val results = sites.map { site ->
            async(Dispatchers.IO) {
                if (key.isBlank()) return@async emptyList<IndexedListing>() to SearchSourceStatus(site.name, 0, "Индекс не настроен")
                try {
                    val url = "https://api.search.brave.com/res/v1/web/search".toHttpUrl().newBuilder()
                        .addQueryParameter("q", "site:${site.domain} ${phrase(q)}")
                        .addQueryParameter("search_lang", "ru").addQueryParameter("count", "10")
                        .addQueryParameter("offset", page.coerceIn(0, 9).toString()).build()
                    val request = Request.Builder().url(url).header("Accept", "application/json")
                        .header("X-Subscription-Token", key).build()
                    waitForSlot()
                    var response = http.newCall(request).execute()
                    if (response.code == 429) {
                        burstRetrySeconds(response.header("X-RateLimit-Remaining"), response.header("X-RateLimit-Reset"),
                            response.header("Retry-After"))?.let { wait ->
                            response.close()
                            delay(wait * 1_000L)
                            waitForSlot()
                            response = http.newCall(request).execute()
                        }
                    }
                    response.use {
                        if (!it.isSuccessful) error(when(it.code) {
                            401, 403 -> "Brave отклонил ключ или тариф"
                            429 -> "Brave ограничил запросы или исчерпана квота"
                            else -> "Поисковый индекс: HTTP ${it.code}"
                        })
                        val body = checkNotNull(it.body).source()
                        check(!body.request(1_000_001)) { "Ответ индекса слишком большой" }
                        val list = JsonParser.parseString(body.readUtf8()).asJsonObject
                            .getAsJsonObject("web")?.getAsJsonArray("results")
                        val found = list.orEmpty().mapNotNull { r ->
                            val data = r.asJsonObject
                            val link = canonical(site, data.get("url")?.asString.orEmpty()) ?: return@mapNotNull null
                            val title = data.get("title")?.asString?.replace(Regex("<[^>]+>"), "")?.take(170)
                                ?.takeIf { GeoListingText.isApartment(it, site.name) } ?: return@mapNotNull null
                            IndexedListing(site.name,title,link)
                        }.distinctBy { it.url }
                        found to SearchSourceStatus(site.name, found.size,
                            if (found.isNotEmpty()) "Индекс · нужна проверка объявления"
                            else "Индекс ответил · прямых ссылок на квартиры нет")
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    val detail = if (e is java.io.IOException) "Нет ответа от индекса" else e.message.orEmpty().take(130)
                    emptyList<IndexedListing>() to SearchSourceStatus(site.name, 0, detail)
                }
            }
        }.awaitAll()
        val unseen = results.map { (links, _) -> links.filter { "index:${it.url}" !in seen && it.url !in seen &&
            ListingOrigin.original(Offer(source = it.source, url = it.url))?.identity !in seen } }
        val fresh = roundRobin(unseen).distinctBy { it.url }
        IndexResult(fresh.take(24), results.map { it.second })
    }

    internal fun <T : Any> roundRobin(groups: List<List<T>>): List<T> =
        (0 until (groups.maxOfOrNull { it.size } ?: 0)).flatMap { i -> groups.mapNotNull { it.getOrNull(i) } }

    private fun com.google.gson.JsonArray?.orEmpty(): Iterable<com.google.gson.JsonElement> = this ?: emptyList()
}

private object GeoListingText {
    private val apartment = Regex("квартир|студи|\\d[ -]*(?:комн|к\\.)", RegexOption.IGNORE_CASE)
    private val rent = Regex("аренда|сда[её]тся|снять|посуточно|объявление (?:снято|закрыто)|в архиве", RegexOption.IGNORE_CASE)
    private val sale = Regex("прод|купить|₽|руб", RegexOption.IGNORE_CASE)
    fun isApartment(title: String, source: String): Boolean = !rent.containsMatchIn(title) && apartment.containsMatchIn(title) &&
        (source !in setOf("Telegram · публичные сообщения", "ВКонтакте · публичные записи") || sale.containsMatchIn(title))
}
