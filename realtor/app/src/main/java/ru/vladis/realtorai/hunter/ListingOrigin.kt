package ru.vladis.realtorai.hunter

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.Instant
import java.time.OffsetDateTime

object ListingOrigin {
    data class Original(val url: String, val id: String, val identity: String)
    private val numeric = Regex("[0-9]{5,}")
    private val slugId = Regex("(?:.*_)?([0-9]{5,})")
    private val lotId = Regex("[A-Za-z0-9_-]{4,100}")
    private val uuid = Regex("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
    private val phone = Regex("\\+?[0-9() .-]{10,24}")

    fun webUrl(raw: String): HttpUrl? = raw.trim().takeIf { it.length in 9..4096 }
        ?.toHttpUrlOrNull()?.takeIf { it.isHttps && it.username.isEmpty() && it.password.isEmpty() }

    fun original(offer: Offer): Original? {
        val url = webUrl(offer.url) ?: return null
        val host = url.host
        val path = url.pathSegments.dropLastWhile(String::isEmpty)
        val last = path.lastOrNull() ?: return null
        fun hostIs(domain: String) = host == domain || host.endsWith(".$domain")
        val id = when {
            hostIs("cian.ru") -> last.takeIf { path.size == 3 && path[0] in listOf("sale", "rent") &&
                path[1] in listOf("flat", "suburban", "room", "commercial") && it.matches(numeric) }
            hostIs("avito.ru") -> slugId.matchEntire(last)?.groupValues?.get(1)?.takeIf {
                path.size == 3 && path[1] in listOf("kvartiry", "komnaty", "doma_dachi_kottedzhi")
            }
            hostIs("domclick.ru") -> last.takeIf { path.size == 3 && path[0] == "card" && it.matches(uuid) }
            hostIs("realty.yandex.ru") -> last.takeIf { path.size == 2 && path[0] == "offer" && it.matches(numeric) }
            hostIs("move.ru") -> slugId.matchEntire(last)?.groupValues?.get(1)?.takeIf { path.size == 2 && path[0] == "objects" }
            hostIs("mirkvartir.ru") -> last.takeIf { path.size == 1 && it.matches(numeric) }
            hostIs("torgiopen.ru") -> last.takeIf { path.size == 2 && path[0] == "lot" && it.matches(lotId) }
            else -> offer.externalId.trim().takeIf { it.matches(lotId) && path.size >= 2 && last == it && path[path.lastIndex - 1] in setOf("listing", "listings", "offer", "offers", "property", "properties", "object", "objects") }
        } ?: return null
        val provider = listOf("cian.ru", "avito.ru", "domclick.ru", "realty.yandex.ru", "move.ru", "mirkvartir.ru", "torgiopen.ru")
            .firstOrNull(::hostIs) ?: host.removePrefix("www.")
        return Original(url.newBuilder().query(null).fragment(null).build().toString(), id, "$provider:$id")
    }

    fun photos(value: String?): List<String> = value.orEmpty().lineSequence().flatMap { it.split('|').asSequence() }
        .mapNotNull { webUrl(it)?.newBuilder()?.fragment(null)?.build()?.toString() }
        .distinct().take(24).toList()

    fun dialable(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.matches(phone) } ?: return null
        val digits = raw.filter { it in '0'..'9' }
        return when {
            digits.length == 11 && digits.first() in "78" -> "+7" + digits.drop(1)
            digits.length == 10 && digits.startsWith("9") -> "+7$digits"
            raw.startsWith("+") && digits.length in 10..15 -> "+$digits"
            else -> null
        }
    }

    fun updatedAt(raw: String?): Long? {
        val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return (value.toLongOrNull()?.let { if (it in 1..9_999_999_999L) it * 1000 else it }
            ?: runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull())
            ?.takeIf { it in 946684800000L..4102444800000L }
    }

    fun ownerApartment(offer: Offer): Boolean = offer.seller == "owner" && offer.market == "secondary" &&
        GeoScope.normalize(offer.housingKind.orEmpty()) in setOf("", "квартира", "квартиры", "apartment", "flat")
}
