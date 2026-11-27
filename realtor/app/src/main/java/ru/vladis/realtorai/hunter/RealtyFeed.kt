package ru.vladis.realtorai.hunter

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

internal object RealtyFeed {
    fun parse(input: String, source: String): List<Offer> {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        p.setInput(StringReader(input))
        val result = mutableListOf<Offer>()
        val path = mutableListOf<String>()
        val images = mutableListOf<String>()
        val phones = mutableListOf<String>()
        val stations = mutableListOf<String>()
        var fields = mutableMapOf<String, String>()
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> {
                    path += p.name
                    if (path.size == 1) require(p.name == "realty-feed") { "Ожидается XML-фид YRL" }
                    if (path.size == 2 && p.name == "offer") {
                        stations.clear()
                        images.clear()
                        phones.clear()
                        fields = mutableMapOf("id" to p.getAttributeValue(null, "internal-id").orEmpty())
                    }
                }
                XmlPullParser.TEXT -> if (path.getOrNull(1) == "offer" && path.size > 2) {
                    val key = path.drop(2).joinToString("/")
                    fields[key] = (fields[key].orEmpty() + p.text).trim()
                }
                XmlPullParser.END_TAG -> {
                    val field = path.drop(2).joinToString("/")
                    if (field in setOf("location/metro/name","location/railway-station/name","location/mck/name","location/mcd/name")) {
                        fields.remove(field)?.takeIf { it.isNotBlank() }?.let(stations::add)
                    }
                    if (field == "image") fields.remove(field)?.takeIf(String::isNotBlank)?.let(images::add)
                    if (field == "sales-agent/phone") fields.remove(field)?.let(phones::add)
                    if (path.size == 2 && p.name == "offer") {
                        fun value(key: String) = fields[key].orEmpty()
                        if (value("price/currency") in setOf("RUB","RUR","") && value("type") == "продажа" &&
                            value("price/value").toLongOrNull()?.let { it > 0 } == true &&
                            value("area/value").replace(',','.').toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true &&
                            (value("id").isNotBlank() || value("url").isNotBlank())) {
                            HunterImport.rowToOffer(mapOf("external_id" to value("id"), "url" to value("url"),
                                "title" to value("building-name").ifBlank { value("category").replaceFirstChar { it.uppercase() } },
                                "address" to listOf("location/locality-name","location/address").map(::value).filter(String::isNotBlank).joinToString(", "),
                                "district" to value("location/sub-locality-name"),
                                "region" to value("location/region"), "locality" to value("location/locality-name"),
                                "lat" to value("location/latitude"), "lon" to value("location/longitude"),
                                "transit" to stations.distinct().joinToString(";"), "seller" to value("sales-agent/category"),
                                "photos" to images.joinToString("\n"), "description" to value("description"),
                                "updated_at" to value("last-update-date"), "owner_name" to value("sales-agent/name"),
                                "owner_phone" to phones.firstNotNullOfOrNull(ListingOrigin::dialable).orEmpty(),
                                "expires_at" to value("expire-date"), "price" to value("price/value"),
                                "area" to value("area/value"), "rooms" to value("rooms"), "floor" to value("floor"),
                                "floors_total" to value("floors-total"), "housing_kind" to value("category"),
                                "market" to if (value("new-flat") in setOf("1","true")) "primary" else "secondary"), source)?.let(result::add)
                        }
                    }
                    path.removeAt(path.lastIndex)
                }
            }
            p.next()
        }
        return result
    }
}
