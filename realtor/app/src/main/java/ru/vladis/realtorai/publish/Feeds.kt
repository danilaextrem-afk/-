package ru.vladis.realtorai.publish

import com.google.gson.GsonBuilder
import ru.vladis.realtorai.ui.rub
import java.io.OutputStream
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Структуры фидов составлены по известным спецификациям, НЕ проверены валидаторами площадок: перед загрузкой проверьте в кабинете. */
object Feeds {
    data class Cfg(val baseUrl: String, val phone: String)
    const val ORG = "Владис"

    fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    fun photoName(l: Listing, i: Int) = "photos/${l.id}_${i + 1}.jpg"
    private fun url(c: Cfg, l: Listing, i: Int) =
        if (c.baseUrl.isBlank()) photoName(l, i) else c.baseUrl.trimEnd('/') + "/" + photoName(l, i).removePrefix("photos/")
    private fun phone(l: Listing, c: Cfg) = l.phone.ifBlank { c.phone }
    private fun digits10(p: String) = p.filter(Char::isDigit).let { if (it.length == 11 && (it[0] == '8' || it[0] == '7')) it.drop(1) else it }
    private const val H = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"

    fun avito(ls: List<Listing>, c: Cfg) = buildString {
        append(H).append("<Ads formatVersion=\"3\" target=\"Avito.ru\">\n")
        ls.forEach { l ->
            append("<Ad><Id>vl-${l.id}</Id><AdStatus>Free</AdStatus><Category>Квартиры</Category><OperationType>Продам</OperationType>")
            append("<MarketType>${if (l.market == "primary") "Новостройка" else "Вторичка"}</MarketType>")
            append("<Address>${esc(l.address)}</Address><Price>${l.price}</Price>")
            if (l.rooms > 0) append("<Rooms>${l.rooms}</Rooms>")
            if (l.area > 0) append("<Square>${l.area}</Square>")
            if (l.kitchenArea > 0) append("<KitchenSpace>${l.kitchenArea}</KitchenSpace>")
            if (l.floor > 0) append("<Floor>${l.floor}</Floor>")
            if (l.floorsTotal > 0) append("<Floors>${l.floorsTotal}</Floors>")
            append("<Description>${esc(l.description)}</Description><ContactPhone>${esc(phone(l, c))}</ContactPhone><Images>")
            l.ordered().forEachIndexed { i, _ -> append("<Image url=\"${esc(url(c, l, i))}\"/>") }
            append("</Images></Ad>\n")
        }
        append("</Ads>\n")
    }

    fun cian(ls: List<Listing>, c: Cfg) = buildString {
        append(H).append("<feed><feed_version>2</feed_version>\n")
        ls.forEach { l ->
            append("<object><Category>flatSale</Category><ExternalId>vl-${l.id}</ExternalId><Description>${esc(l.description)}</Description>")
            append("<Address>${esc(l.address)}</Address>")
            append("<Phones><PhoneSchema><CountryCode>+7</CountryCode><Number>${digits10(phone(l, c))}</Number></PhoneSchema></Phones>")
            if (l.rooms > 0) append("<FlatRoomsCount>${l.rooms}</FlatRoomsCount>")
            if (l.area > 0) append("<TotalArea>${l.area}</TotalArea>")
            if (l.kitchenArea > 0) append("<KitchenArea>${l.kitchenArea}</KitchenArea>")
            if (l.floor > 0) append("<FloorNumber>${l.floor}</FloorNumber>")
            if (l.floorsTotal > 0) append("<Building><FloorsCount>${l.floorsTotal}</FloorsCount></Building>")
            append("<BargainTerms><Price>${l.price}</Price><Currency>rur</Currency></BargainTerms><Photos>")
            l.ordered().forEachIndexed { i, _ -> append("<PhotoSchema><FullUrl>${esc(url(c, l, i))}</FullUrl><IsDefault>${i == 0}</IsDefault></PhotoSchema>") }
            append("</Photos></object>\n")
        }
        append("</feed>\n")
    }

    /** Формат Яндекс.Недвижимости; для Домклик используется та же структура — сверьте со спецификацией Домклик (партнёрский доступ). */
    fun yandex(ls: List<Listing>, c: Cfg) = buildString {
        append(H).append("<realty-feed xmlns=\"http://webmaster.yandex.ru/schemas/feed/realty/2010-06\"><generation-date>${OffsetDateTime.now()}</generation-date>\n")
        ls.forEach { l ->
            append("<offer internal-id=\"${l.id}\"><type>продажа</type><property-type>жилая</property-type><category>квартира</category>")
            if (l.market == "primary") append("<new-flat>1</new-flat>")
            append("<creation-date>${OffsetDateTime.now()}</creation-date>")
            append("<location><country>Россия</country><address>${esc(l.address)}</address></location>")
            append("<sales-agent><phone>${esc(phone(l, c))}</phone><category>agency</category><organization>$ORG</organization></sales-agent>")
            append("<price><value>${l.price}</value><currency>RUB</currency></price>")
            if (l.area > 0) append("<area><value>${l.area}</value><unit>кв. м</unit></area>")
            if (l.kitchenArea > 0) append("<kitchen-space><value>${l.kitchenArea}</value><unit>кв. м</unit></kitchen-space>")
            if (l.rooms > 0) append("<rooms>${l.rooms}</rooms>")
            if (l.floor > 0) append("<floor>${l.floor}</floor>")
            if (l.floorsTotal > 0) append("<floors-total>${l.floorsTotal}</floors-total>")
            append("<description>${esc(l.description)}</description>")
            l.ordered().forEachIndexed { i, _ -> append("<image>${esc(url(c, l, i))}</image>") }
            append("</offer>\n")
        }
        append("</realty-feed>\n")
    }

    /** ZIP: фиды выбранных площадок (без проданных) + фото + готовые тексты + listings.json (формат для VT 24/ручной загрузки). */
    fun writeZip(out: OutputStream, all: List<Listing>, platforms: Set<Platform>, c: Cfg) {
        val ls = all.filter { it.status != "sold" }
        require(ls.all { Checks.blocking(it).isEmpty() }) { "Заполните обязательные поля всех выбранных объектов" }
        ZipOutputStream(out).use { z ->
            fun put(name: String, b: ByteArray) { z.putNextEntry(ZipEntry(name)); z.write(b); z.closeEntry() }
            if (Platform.AVITO in platforms) put("feed_avito.xml", avito(ls, c).toByteArray())
            if (Platform.CIAN in platforms) put("feed_cian.xml", cian(ls, c).toByteArray())
            if (Platform.YANDEX in platforms) put("feed_yandex.xml", yandex(ls, c).toByteArray())
            if (Platform.DOMCLICK in platforms) put("draft_domclick_UNVERIFIED.xml", yandex(ls, c).toByteArray())
            ls.forEach { l ->
                put("listings/${l.id}.txt", l.postText().toByteArray())
                l.ordered().forEachIndexed { i, p -> put(photoName(l, i), Img.jpeg(p, 2000, 85)) }
            }
            val json = ls.map { l -> mapOf("id" to l.id, "title" to l.title, "description" to l.description, "price" to l.price,
                "area" to l.area, "rooms" to l.rooms, "floor" to l.floor, "floorsTotal" to l.floorsTotal,
                "address" to l.address, "market" to l.market, "phone" to phone(l, c), "photos" to l.ordered().indices.map { photoName(l, it) }) }
            put("listings.json", GsonBuilder().setPrettyPrinting().create().toJson(json).toByteArray())
            put("README.txt", ("Это файл выгрузки, а не публикация. Ничего не опубликовано, пока вы не загрузите фид в личный кабинет площадки и она его не примет.\n" +
                "Фиды ссылаются на фото по URL: загрузите папку photos на свой хостинг и укажите её адрес в настройках приложения (сейчас " +
                (if (c.baseUrl.isBlank()) "адрес НЕ указан — в фидах относительные пути" else c.baseUrl) + ").\n" +
                "ВНИМАНИЕ: выборочная выгрузка не является полным фидом. Не заменяйте ей существующий полный фид: можно снять другие объявления. Шаблон Домклик не проверен.\nПлатное продвижение не включено (Авито: AdStatus=Free). Спецификации фидов проверяйте по актуальной документации площадок.\n").toByteArray())
        }
    }
}

fun Listing.postText(): String = buildString {
    appendLine(title.ifBlank { "Продаётся квартира" }); appendLine()
    appendLine("Цена: ${price.rub()}")
    val spec = listOfNotNull(if (rooms > 0) "$rooms-комн." else null, if (area > 0) "$area м²" else null,
        if (floor > 0) "этаж $floor" + (if (floorsTotal > 0) " из $floorsTotal" else "") else null)
    if (spec.isNotEmpty()) appendLine(spec.joinToString(" · "))
    if (kitchenArea > 0) appendLine("Кухня: $kitchenArea м²")
    if (address.isNotBlank()) appendLine("Адрес: $address")
    if (description.isNotBlank()) { appendLine(); appendLine(description) }
    if (phone.isNotBlank()) { appendLine(); append("Тел.: $phone") }
}.trim()

object Checks {
    fun blocking(l: Listing): List<String> = listOfNotNull(
        if (l.price <= 0) "цена" else null, if (!l.area.isFinite() || l.area <= 0) "площадь" else null,
        if (l.address.isBlank()) "адрес" else null, if (l.title.isBlank()) "заголовок" else null,
        if (l.description.isBlank()) "описание" else null,
        if (!l.kitchenArea.isFinite() || l.kitchenArea < 0 || l.kitchenArea > l.area) "площадь кухни" else null,
        if (l.rooms < 0 || l.floor < 0 || l.floorsTotal < 0 || (l.floorsTotal > 0 && l.floor > l.floorsTotal)) "комнаты/этаж" else null)
    fun warnings(l: Listing): List<String> = listOfNotNull(
        if (l.photoList().size < 3) "Мало фото (${l.photoList().size}) — объявления с 5+ фото работают лучше" else null,
        if (l.title.length > 50) "Заголовок ${l.title.length} симв. — длиннее лимита Авито (50)" else null,
        if (l.postText().length > 1024 && l.photoList().isNotEmpty()) "Текст >1024 симв.: сократите перед публикацией с фото в Telegram" else null,
        if (l.rooms <= 0 || l.floor <= 0) "Не указаны комнаты или этаж" else null)
}
