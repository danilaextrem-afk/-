package ru.vladis.realtorai.hunter

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import ru.vladis.realtorai.data.Property
import java.util.Locale
import kotlin.math.*

object GeoScope {
    const val MOSCOW = "moscow"
    const val OBLAST = "oblast"
    private const val OUTSIDE = "outside"
    private val city = Regex("^(?:(?:г\\.?|город)\\s*)?(?:москва|москвы)(?:$|[,;.! ])")
    private val oblast = Regex("^(?:московская обл(?:асть)?\\.?|подмосковье|московской области|мо)(?:$|[,;.! ])")
    private val newMoscow = Regex("\\b(?:новая москва|новой москве|тинао|новомосковск(?:ий|ого) административн(?:ый|ого) округ|троицк(?:ий|ого) административн(?:ый|ого) округ)\\b")
    private val prefix = Regex("^(?:(?:российская федерация|россия|рф|[0-9]{6})[,; ]+)+")
    private val staleMs = 30 * 86_400_000L
    @Volatile private var boundaries: Map<String, List<List<Ring>>>? = null
    @Volatile private var mkad: List<List<Point>> = emptyList()

    data class Point(val lat: Double, val lon: Double)
    private class Ring(val points: List<Point>) {
        private val south = points.minOf { it.lat }
        private val north = points.maxOf { it.lat }
        private val west = points.minOf { it.lon }
        private val east = points.maxOf { it.lon }
        fun contains(point: Point): Boolean {
            if (point.lat !in south..north || point.lon !in west..east) return false
            var inside = false
            for (i in 1 until points.size) {
                val a = points[i - 1]
                val b = points[i]
                if ((a.lat > point.lat) != (b.lat > point.lat) &&
                    point.lon < (b.lon - a.lon) * (point.lat - a.lat) / (b.lat - a.lat) + a.lon) inside = !inside
            }
            return inside
        }
    }

    fun initialize(ctx: Context) {
        if (boundaries != null) return
        synchronized(this) {
            if (boundaries != null) return
            val data = readGeometry(ctx, "moscow-boundaries.json").asJsonObject
            val parsed = data.entrySet().associate { (key, polygons) ->
                key to polygons.asJsonArray.map { polygon ->
                    polygon.asJsonArray.mapNotNull { ring ->
                        val points = ring.asJsonArray.map { pair -> Point(pair.asJsonArray[1].asDouble, pair.asJsonArray[0].asDouble) }
                        if (points.size < 4) null else Ring(points)
                    }
                }
            }
            mkad = readGeometry(ctx, "mkad.json").asJsonArray.map { way ->
                way.asJsonArray.map { pair -> Point(pair.asJsonArray[0].asDouble, pair.asJsonArray[1].asDouble) }
            }
            boundaries = parsed
        }
    }

    private fun readGeometry(ctx: Context, name: String): com.google.gson.JsonElement {
        // AAPT expands .gz assets and removes the suffix when packaging.
        val stream = try {
            ctx.assets.open(name)
        } catch (_: java.io.FileNotFoundException) {
            java.util.zip.GZIPInputStream(ctx.assets.open("$name.gz"))
        }
        return stream.bufferedReader().use { JsonParser.parseReader(it) }
    }

    fun normalize(value: String) = value.lowercase(Locale.ROOT).replace('ё','е').trim().replace(Regex("\\s+"), " ")

    fun addressMatches(value: String, fragment: String): Boolean {
        val strip = Regex("[^\\p{L}\\p{N}]+")
        fun canonical(text: String) = normalize(text).replace(strip, " ").trim()
        val query = canonical(fragment)
        return query.isBlank() || (" " + canonical(value) + " ").contains(" $query ")
    }

    fun textRegion(value: String): String? {
        val text = normalize(value).replace(prefix, "")
        return when {
            text == MOSCOW || city.containsMatchIn(text) || newMoscow.find(text)?.range?.first == 0 -> MOSCOW
            text == OBLAST || oblast.containsMatchIn(text) -> OBLAST
            else -> null
        }
    }

    fun classify(address: String, declared: String?, lat: Double?, lon: Double?): String? {
        val stated = declared?.trim()?.takeIf(String::isNotEmpty)?.let { textRegion(it) ?: OUTSIDE }
        if (stated == OUTSIDE) return null
        val addressRegion = textRegion(address)
        if (stated != null && addressRegion != null && stated != addressRegion) return null
        val point = point(lat, lon)
        val polygons = boundaries
        if (point != null && polygons != null) {
            val actual = listOf(MOSCOW, OBLAST).firstOrNull { id ->
                polygons[id].orEmpty().any { rings -> rings.firstOrNull()?.contains(point) == true && rings.drop(1).none { it.contains(point) } }
            } ?: return null
            if (stated != null && stated != actual || addressRegion != null && addressRegion != actual) return null
            return actual
        }
        return stated ?: addressRegion
    }

    fun region(offer: Offer) = classify(offer.address, offer.region, offer.lat, offer.lon)
    fun region(property: Property) = classify(property.address, property.region, null, null)
    fun eligible(offer: Offer, now: Long = System.currentTimeMillis()): Boolean =
        offer.active && offer.lastSeen >= now - staleMs && (offer.expiresAt == null || offer.expiresAt > now) && region(offer) != null

    fun searchableProperties(properties: List<Property>, offers: List<Offer>, scope: GeoQuery): List<Property> {
        val bySource = HashMap<String, Offer>()
        val byUrl = HashMap<String, Offer>()
        val activeIds = HashSet<Long>()
        for (offer in offers) {
            if (!eligible(offer) || !scope.matches(offer)) continue
            activeIds += offer.id
            ru.vladis.realtorai.autopilot.AutoRules.sourceKey(offer)?.let { bySource[it] = offer }
            ru.vladis.realtorai.autopilot.AutoRules.urlKey(offer.url)?.let { byUrl[it] = offer }
        }
        return properties.filter { property ->
            val source = property.description.lineSequence().firstOrNull { it.startsWith("[AUTO-SOURCE:") }
            val tracked = source != null || property.description.lineSequence().firstOrNull()?.startsWith("Источник: ") == true
            val link = ru.vladis.realtorai.autopilot.AutoRules.urlKey(property.url)
            val sourceId = property.sourceOfferId
            if (sourceId != null) sourceId in activeIds
            else if (tracked) (source != null && bySource.containsKey(source)) || (link != null && byUrl.containsKey(link))
            else scope.matches(Offer(source = "CRM", address = property.address, region = property.region))
        }
    }

    fun point(lat: Double?, lon: Double?): Point? =
        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) Point(lat,lon) else null

    fun km(a: Point, b: Point): Double {
        val from = Math.toRadians(a.lat)
        val to = Math.toRadians(b.lat)
        val delta = sin((to - from) / 2).pow(2) + cos(from) * cos(to) * sin(Math.toRadians(b.lon - a.lon) / 2).pow(2)
        return 6371.0088 * 2 * asin(sqrt(delta.coerceIn(0.0,1.0)))
    }

    fun mkadKm(point: Point): Double? {
        val scale = cos(Math.toRadians(point.lat))
        var best = Double.POSITIVE_INFINITY
        for (way in mkad) {
            for (index in 1 until way.size) {
                val a = way[index - 1]
                val b = way[index]
                val x = (b.lon - a.lon) * scale
                val y = b.lat - a.lat
                val length = x * x + y * y
                val position = if (length > 0) (((point.lon - a.lon) * scale * x + (point.lat - a.lat) * y) / length).coerceIn(0.0,1.0) else 0.0
                val dx = (point.lon - a.lon - position * (b.lon - a.lon)) * scale
                val dy = point.lat - a.lat - position * y
                best = min(best, dx * dx + dy * dy)
            }
        }
        return if (best.isFinite()) Math.toRadians(sqrt(best)) * 6371.0088 else null
    }

    private val districtEnding = Regex("(?:ском|ского|скому|ский|ская|ское|ской)$")
    private fun locationKey(value: String) = normalize(value)
        .split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() && it !in setOf("район","районе","района","г","город","округ","округе","городской") }
        .joinToString(" ") { it.replace(districtEnding,"ск") }
    fun within(place: String, offer: Offer): Boolean {
        val point = point(offer.lat, offer.lon)
        if (newMoscow.containsMatchIn(normalize(place)) && point != null && boundaries != null) {
            return region(offer) == MOSCOW && boundaries?.get("new-moscow").orEmpty().any { rings ->
                rings.firstOrNull()?.contains(point) == true && rings.drop(1).none { it.contains(point) }
            }
        }
        return within(place, region(offer), offer.locality, offer.district, offer.address)
    }

    fun within(place: String, region: String?, locality: String?, district: String, address: String): Boolean {
        if (place.isBlank()) return true
        val desired = normalize(place)
        if (newMoscow.containsMatchIn(desired)) return region == MOSCOW && newMoscow.containsMatchIn(normalize("$locality $district $address"))
        if (textRegion(desired) != null) return region == textRegion(desired)
        val key = locationKey(place)
        return key.isNotBlank() && listOfNotNull(locality,district,address).any { (" " + locationKey(it) + " ").contains(" $key ") }
    }
}

data class GeoQuery(
    val locations: List<String> = emptyList(), val stations: List<String> = emptyList(),
    val address: String = "", val center: GeoScope.Point? = null,
    val radiusKm: Double? = null, val mkadKm: Double? = null
) {
    fun matches(offer: Offer): Boolean {
        if (GeoScope.region(offer) == null) return false
        if (locations.isNotEmpty() && locations.none { GeoScope.within(it, offer) }) return false
        if (address.isNotBlank() && !GeoScope.addressMatches(offer.address, address)) return false
        val stationKeys = offer.transit.orEmpty().split(';').map(GeoScope::normalize)
        if (stations.isNotEmpty() && stations.none { GeoScope.normalize(it) in stationKeys }) return false
        val point = GeoScope.point(offer.lat, offer.lon)
        if (radiusKm != null && (point == null || center == null || GeoScope.km(center, point) > radiusKm)) return false
        if (mkadKm != null && (point == null || GeoScope.mkadKm(point)?.let { it <= mkadKm } != true)) return false
        return true
    }
}

object GeoPreferences {
    val default = GeoQuery()
    fun read(ctx: Context): GeoQuery {
        val p = ctx.getSharedPreferences("work_geography", Context.MODE_PRIVATE)
        return GeoQuery(split(p.getString("areas", "").orEmpty()), split(p.getString("stations", "").orEmpty()),
            p.getString("address", "").orEmpty(),
            GeoScope.point(p.getString("lat", null)?.toDoubleOrNull(), p.getString("lon", null)?.toDoubleOrNull()),
            p.getString("radius", null)?.toDoubleOrNull(), p.getString("mkad", null)?.toDoubleOrNull())
    }
    fun observe(ctx: Context) = callbackFlow {
        val preferences = ctx.getSharedPreferences("work_geography", Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read(ctx)) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(read(ctx))
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun split(value: String) = value.split(';').map(String::trim).filter(String::isNotBlank).distinct().take(40)
    fun save(ctx: Context, query: GeoQuery) {
        ctx.getSharedPreferences("work_geography", Context.MODE_PRIVATE).edit()
            .putString("areas", query.locations.joinToString(";")).putString("stations", query.stations.joinToString(";"))
            .putString("address", query.address).putString("lat", query.center?.lat?.toString())
            .putString("lon", query.center?.lon?.toString()).putString("radius", query.radiusKm?.toString())
            .putString("mkad", query.mkadKm?.toString()).apply()
    }
}
