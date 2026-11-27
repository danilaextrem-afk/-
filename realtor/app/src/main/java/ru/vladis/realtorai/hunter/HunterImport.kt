package ru.vladis.realtorai.hunter

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import ru.vladis.realtorai.ai.Importer

object HunterImport {
    data class Plan(val inserts: List<Offer>, val updates: List<Pair<Offer, Boolean>>, val skipped: Int)
    data class Result(val newIds: List<Long>, val changedIds: List<Long>, val dropIds: List<Long>, val unchanged: Int, val skipped: Int) {
        fun describe() = "Новых: ${newIds.size}, цена изменилась: ${changedIds.size}, без изменений: $unchanged, пропущено (регион, срок или данные): $skipped"
    }

    // ---------- CSV ----------
    fun parseCsv(text: String): List<List<String>> {
        val t = text.trimStart('\uFEFF'); val first = t.lineSequence().firstOrNull().orEmpty()
        val d = listOf(';', ',', '\t').maxByOrNull { c -> first.count { it == c } } ?: ','
        val rows = mutableListOf<List<String>>(); var row = mutableListOf<String>(); val cur = StringBuilder(); var q = false; var i = 0
        while (i < t.length) {
            val c = t[i]
            when {
                q && c == '"' && i + 1 < t.length && t[i + 1] == '"' -> { cur.append('"'); i++ }
                c == '"' -> q = !q
                !q && c == d -> { row.add(cur.toString()); cur.clear() }
                !q && (c == '\n' || c == '\r') -> { if (c == '\r' && i + 1 < t.length && t[i + 1] == '\n') i++; row.add(cur.toString()); cur.clear(); if (row.any { it.isNotBlank() }) rows.add(row); row = mutableListOf() }
                else -> cur.append(c)
            }
            i++
        }
        row.add(cur.toString()); if (row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }

    private val alias = mapOf(
        "price" to listOf("цена", "price"), "area" to listOf("площадь", "area", "total_area"), "rooms" to listOf("комнат", "комнаты", "rooms"),
        "floor" to listOf("этаж", "floor"), "floors" to listOf("этажность", "этажей", "floors", "floors_total"),
        "address" to listOf("адрес", "address"), "district" to listOf("район", "district"),
        "lat" to listOf("lat", "широта"), "lon" to listOf("lon", "lng", "долгота"), "url" to listOf("ссылка", "url", "link"),
        "housing_kind" to listOf("тип жилья", "housing_kind"),
        "region" to listOf("регион", "region"), "locality" to listOf("город", "locality", "city"),
        "transit" to listOf("станции", "transit", "метро", "metro", "stations"),
        "seller" to listOf("продавец", "seller", "seller_type"),
        "expiry" to listOf("действует до", "expires_at", "expiresAt", "expiresat"),
        "status" to listOf("статус", "status", "active"),
        "owner_name" to listOf("owner_name", "ownername", "contact_name", "имя собственника"),
        "owner_phone" to listOf("owner_phone", "ownerphone", "contact_phone", "телефон собственника"),
        "crm_allowed" to listOf("crm_allowed", "разрешен импорт контакта"),
        "photos" to listOf("photos", "photo_urls", "images", "image", "photo", "фото"),
        "description" to listOf("description", "описание"),
        "updated_at" to listOf("updated_at", "updatedat", "last_updated", "lastupdated", "last_update_date", "дата обновления"),
        "id" to listOf("id", "external_id", "listing_id"), "title" to listOf("заголовок", "title"), "deal" to listOf("сделка", "deal"), "market" to listOf("рынок", "market"))

    private fun num(s: String?): Double? = s?.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }?.replace(',', '.')?.toDoubleOrNull()

    fun rowToOffer(m: Map<String, String>, source: String): Offer? {
        fun g(k: String) = alias[k]!!.firstNotNullOfOrNull { a -> m[a]?.trim()?.takeIf { it.isNotEmpty() } }
        val floorRaw = g("floor").orEmpty()
        val floor = num(floorRaw.substringBefore('/'))?.toInt() ?: 0
        val total = num(g("floors"))?.toInt() ?: num(floorRaw.substringAfter('/', ""))?.toInt() ?: 0
        val deal = g("deal").orEmpty().lowercase().let { if ("аренд" in it || "rent" in it) "rent" else "sale" }
        val market = g("market").orEmpty().lowercase().let { if ("перв" in it || "новостр" in it || "primary" in it) "primary" else "secondary" }
        val allowed = g("crm_allowed")?.lowercase() in setOf("true", "да", "1")
        val seller = when (GeoScope.normalize(g("seller").orEmpty())) {
            "owner", "private", "собственник", "владелец" -> "owner"
            "developer", "застройщик" -> "developer"
            "agent", "agency", "агентство", "агент" -> "agent"
            else -> null
        }
        val expired = g("status")?.lowercase() in setOf("false", "0", "inactive", "archived", "sold", "closed", "продано", "снято")
        val deadline = g("expiry")?.let { it.toLongOrNull() ?: runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        val o = Offer(source = source, housingKind = g("housing_kind"), seller = seller,
            region = g("region"), locality = g("locality"), transit = g("transit"), expiresAt = deadline, active = !expired,
            ownerName = g("owner_name")?.takeIf { allowed }, ownerPhone = g("owner_phone")?.takeIf { allowed },
            contactName = g("owner_name")?.takeIf { seller == "owner" }?.take(150),
            contactPhone = ListingOrigin.dialable(g("owner_phone"))?.takeIf { seller == "owner" },
            photoUrls = ListingOrigin.photos(g("photos")).joinToString("\n").takeIf(String::isNotEmpty),
            sourceDescription = g("description")?.take(12_000), sourceUpdatedAt = ListingOrigin.updatedAt(g("updated_at")),
            externalId = g("id").orEmpty().take(250), url = g("url").orEmpty().take(4096), title = g("title").orEmpty(),
            address = g("address").orEmpty(), district = g("district").orEmpty(), lat = num(g("lat")), lon = num(g("lon")),
            deal = deal, market = market, price = num(g("price"))?.toLong() ?: 0, area = num(g("area")) ?: 0.0,
            rooms = num(g("rooms"))?.toInt() ?: 0, floor = floor, floorsTotal = total)
        return o.copy(fingerprint = Dedup.fp(o))
    }

    fun fromCsv(text: String, source: String): List<Offer> {
        val rows = parseCsv(text); if (rows.size < 2) return emptyList()
        val head = rows[0].map { it.trim().lowercase() }
        return rows.drop(1).mapNotNull { r -> rowToOffer(head.indices.associate { head[it] to r.getOrElse(it) { "" } }, source) }
    }

    fun fromJson(text: String, source: String): List<Offer> {
        val root = JsonParser.parseString(text)
        val arr: JsonArray = if (root.isJsonArray) root.asJsonArray else root.asJsonObject.let { o ->
            listOf("offers", "items", "data", "ads").firstNotNullOfOrNull { k -> o.get(k)?.takeIf { it.isJsonArray }?.asJsonArray } ?: JsonArray() }
        return arr.mapNotNull { e ->
            if (!e.isJsonObject) null else rowToOffer(e.asJsonObject.entrySet().associate { it.key.lowercase() to (when {
                it.value.isJsonPrimitive -> it.value.asString
                it.value.isJsonArray && it.key.lowercase() in alias.getValue("photos") ->
                    it.value.asJsonArray.mapNotNull { photo -> when {
                        photo.isJsonPrimitive -> photo.asString
                        photo.isJsonObject -> photo.asJsonObject.get("url")?.takeIf { link -> link.isJsonPrimitive }?.asString
                        else -> null
                    } }.joinToString("\n")
                else -> ""
            }) }, source)
        }
    }

    /** Вставленный текст объявления: простой разбор цены/площади/комнат/этажа (без выдумывания остального). */
    fun fromText(text: String, url: String): Offer {
        val p = Importer.parse(text, url)
        val addr = Regex("Адрес[:\\s]+(.+)").find(text)?.groupValues?.get(1)?.trim().orEmpty()
        val o = Offer(source = "paste", url = url, title = p.title, address = addr, sourceDescription = text.take(12_000), price = p.price, area = p.area, rooms = p.rooms, floor = p.floor)
        return o.copy(fingerprint = Dedup.fp(o))
    }

    // ---------- Применение к базе ----------
    fun plan(existing: List<Offer>, rows: List<Offer>, now: Long): Plan {
        val known = HashMap<String, Offer>()
        fun keys(o: Offer) = listOfNotNull(o.externalId.takeIf { it.isNotBlank() }?.let { "${o.source}|e|$it" },
            ListingOrigin.original(o)?.identity?.let { "${o.source}|u|$it" }, o.fingerprint.takeIf { it.length > 6 }?.let { "${o.source}|f|$it" })
        existing.forEach { o -> keys(o).forEach { known[it] = o } }
        val ins = mutableListOf<Offer>(); val upd = mutableListOf<Pair<Offer, Boolean>>(); var skipped = 0
        val updatedIds = mutableSetOf<Long>()
        for (raw in rows) {
            val region = GeoScope.region(raw)
            if (region == null || !raw.active || raw.expiresAt?.let { it <= now } == true) { skipped++; continue }
            val r = raw.copy(region = region)
            if (r.price <= 0 || !r.area.isFinite() || r.area <= 0) { skipped++; continue }
            val old = keys(r).firstNotNullOfOrNull { known[it] }
            if (old == null) { val n = r.copy(firstSeen = now, lastSeen = now); ins += n; keys(n).forEach { known[it] = n } }
            else if (old.id == 0L || !updatedIds.add(old.id)) skipped++ // повтор внутри того же файла
            else upd += old.copy(externalId = r.externalId.ifBlank { old.externalId },
                photoUrls = r.photoUrls, sourceDescription = r.sourceDescription ?: old.sourceDescription,
                sourceUpdatedAt = r.sourceUpdatedAt ?: old.sourceUpdatedAt, contactName = r.contactName, contactPhone = r.contactPhone,
                region = r.region, locality = r.locality ?: old.locality, transit = r.transit ?: old.transit, seller = r.seller, expiresAt = r.expiresAt, housingKind = r.housingKind ?: old.housingKind, ownerName = r.ownerName, ownerPhone = r.ownerPhone, price = r.price, area = r.area, market = r.market, rooms = r.rooms.takeIf { it > 0 } ?: old.rooms, floor = r.floor.takeIf { it > 0 } ?: old.floor, lastSeen = now, active = true, title = r.title.ifBlank { old.title },
                address = r.address.ifBlank { old.address }, district = r.district.ifBlank { old.district }, url = r.url.ifBlank { old.url },
                lat = r.lat ?: old.lat, lon = r.lon ?: old.lon, floorsTotal = if (r.floorsTotal > 0) r.floorsTotal else old.floorsTotal) to (old.price != r.price)
        }
        return Plan(ins, upd, skipped)
    }

    suspend fun ingest(dao: HunterDao, rows: List<Offer>, now: Long = System.currentTimeMillis()): Result =
        dao.importBatch(rows, now)

    suspend fun applyBatch(dao: HunterDao, rows: List<Offer>, now: Long): Result {
        val old = dao.allOffers().associateBy { it.id }
        for (row in rows) {
            if (row.active && row.expiresAt?.let { it <= now } != true && GeoScope.region(row) != null) continue
            val sourceId = row.externalId.takeIf { it.isNotBlank() }
            val url = ListingOrigin.original(row)?.identity
            val existing = old.values.firstOrNull { it.source == row.source &&
                (sourceId != null && it.externalId == sourceId || url != null && ListingOrigin.original(it)?.identity == url) }
            if (existing?.active == true) dao.saveOffer(existing.copy(active = false))
        }
        val p = plan(old.values.toList(), rows, now)
        val newIds = p.inserts.map { o -> dao.saveOffer(o).also { dao.savePoint(PricePoint(offerId = it, price = o.price, at = now)) } }
        val changed = mutableListOf<Long>(); val drops = mutableListOf<Long>()
        p.updates.forEach { (o, ch) ->
            dao.saveOffer(o)
            if (ch) { dao.savePoint(PricePoint(offerId = o.id, price = o.price, at = now)); changed += o.id; if (o.price < (old[o.id]?.price ?: 0)) drops += o.id }
        }
        return Result(newIds, changed, drops, p.updates.size - changed.size, p.skipped)
    }
}
