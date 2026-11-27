package ru.vladis.realtorai.hunter

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

internal object PartnerFeed {
    fun parse(xml: String, source: String, format: String): List<Offer> {
        val avito = format == "avito"
        require(avito || format == "cian")
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        parser.setInput(StringReader(xml))
        val path = mutableListOf<String>()
        val fields = mutableMapOf<String, String>()
        val images = mutableListOf<String>()
        val phones = mutableListOf<String>()
        val result = mutableListOf<Offer>()
        val root = if (avito) "ads" else "feed"
        val row = if (avito) "ad" else "object"
        var validRoot = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    path += parser.name.lowercase(java.util.Locale.ROOT)
                    if (path.size == 1) {
                        require(path.first() == root) { "Неверный формат фида $format" }
                        validRoot = true
                    }
                    if (path.size == 2 && path.last() == row) {
                        fields.clear()
                        images.clear()
                        phones.clear()
                    }
                    if (path.drop(2).joinToString("/") == "images/image") {
                        parser.getAttributeValue(null, "url")?.takeIf(String::isNotBlank)?.let(images::add)
                    }
                }
                XmlPullParser.TEXT -> if (path.getOrNull(1) == row && path.size > 2) {
                    val key = path.drop(2).joinToString("/")
                    fields[key] = fields[key].orEmpty() + parser.text
                }
                XmlPullParser.END_TAG -> {
                    val field = path.drop(2).joinToString("/")
                    if (field in listOf("photos/photo/fullurl", "photos/photo/url", "images/image", "images/image/url"))
                        fields.remove(field)?.trim()?.takeIf(String::isNotBlank)?.let(images::add)
                    if (field == "phones/phone") {
                        val number = fields.remove("phones/phone/number").orEmpty().trim()
                        val code = fields.remove("phones/phone/countrycode").orEmpty().trim()
                        val combined = if (code.isNotEmpty() && !number.startsWith("+")) "+${code.trimStart('+')}$number" else number
                        ListingOrigin.dialable(combined)?.let(phones::add)
                    }
                    if (path.size == 2 && path.last() == row) map(fields, images, phones, source, avito)?.let(result::add)
                    path.removeAt(path.lastIndex)
                }
            }
            parser.next()
        }
        require(validRoot) { "Пустой XML-фид" }
        return result
    }

    private fun map(fields: Map<String,String>, images: List<String>, phones: List<String>, source: String, avito: Boolean): Offer? {
        fun value(key: String) = fields[key].orEmpty().trim()
        val category = GeoScope.normalize(value("category"))
        if (avito && (category != "квартиры" || GeoScope.normalize(value("operationtype")) !in setOf("продам","продажа"))) return null
        if (!avito && category !in setOf("flatsale","newbuildingflatsale")) return null
        if (!avito && value("bargainterms/currency").lowercase() !in setOf("", "rur", "rub")) return null
        val id = value(if (avito) "id" else "externalid")
        if (id.isBlank()) return null
        return HunterImport.rowToOffer(mapOf(
            "id" to id, "url" to value("url"), "title" to value("title").ifBlank { "Квартира" },
            "housing_kind" to "Квартира", "region" to value("region"), "locality" to value("city"),
            "address" to value("address"), "district" to value("district"),
            "lat" to value(if (avito) "latitude" else "coordinates/lat"), "lon" to value(if (avito) "longitude" else "coordinates/lng"),
            "price" to value(if (avito) "price" else "bargainterms/price"),
            "area" to value(if (avito) "square" else "totalarea"), "rooms" to value(if (avito) "rooms" else "flatroomscount"),
            "floor" to value(if (avito) "floor" else "floornumber"), "floors_total" to value(if (avito) "floors" else "building/floorscount"),
            "market" to if (category == "newbuildingflatsale") "primary" else value("markettype"),
            "seller" to value("sellertype").ifBlank { value("contactmethod/sellertype") },
            "photos" to images.joinToString("\n"), "description" to value("description"),
            "updated_at" to value("updatedat").ifBlank { value("lastupdatedate") },
            "owner_name" to value("ownername").ifBlank { value("managername") },
            "owner_phone" to value("contactphone").ifBlank { phones.firstOrNull().orEmpty() },
            "expires_at" to value("dateend"), "status" to value("status")
        ), source)
    }
}
