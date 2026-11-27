package ru.vladis.realtorai.publish

import androidx.room.withTransaction
import ru.vladis.realtorai.autopilot.AutoRules
import ru.vladis.realtorai.data.*
import ru.vladis.realtorai.hunter.*
import ru.vladis.realtorai.local.OfflineLogic

object SaleIntake {
    private val urlPattern = Regex("""https://[^\s<>()"]+""")
    data class Result(val listingId: Long, val propertyId: Long, val buyers: Int, val pending: Boolean)

    internal fun sharedLink(raw: String): ListingOrigin.Original? = urlPattern.findAll(raw.take(8_000)).firstNotNullOfOrNull {
        ListingOrigin.original(Offer(source = "Поделиться", url = it.value.trimEnd('.', ',', ';', '!', ']')))
    }

    internal fun requested(text: String): Boolean {
        val goal = GeoScope.normalize(text)
        val action = Regex("""(?:^|\s)(?:возьми|взять|добавь|добавить|прими|принять)\s""")
        return action.containsMatchIn(goal) && "продаж" in goal && sharedLink(text) != null
    }

    // Parse only explicit, unambiguous values in Android's shared text. No network
    // access or inferred address, seller, rights, or market classification.
    private data class SharedFacts(val price: Long = 0, val area: Double = 0.0, val rooms: Int = 0,
        val floor: Int = 0, val floorsTotal: Int = 0)

    private fun sharedFacts(raw: String): SharedFacts {
        val links = urlPattern.findAll(raw.take(8_000)).map { it.value }.toList()
        if (links.size != 1) return SharedFacts()
        val text = raw.take(8_000).replace(urlPattern, " ").replace('\u00a0', ' ').replace('\u202f', ' ')
        fun unique(regex: Regex, group: Int = 1): String? =
            regex.findAll(text).map { it.groupValues[group] }.toList().singleOrNull()
        val price = if (Regex("""(?i)(?:₽|руб\.?)\s*/\s*(?:м|кв)""").containsMatchIn(text)) 0L else
            unique(Regex("""(?<![\d.,])(\d{1,3}(?: \d{3})+|\d{5,12})\s*(?:₽|руб(?:лей|\.)?)(?![\p{L}])"""))
                ?.replace(" ", "")?.toLongOrNull()?.takeIf { it in 100_000L..10_000_000_000L } ?: 0L
        val area = unique(Regex("""(?i)(?<![\d.,])(\d{1,4}(?:[.,]\d{1,2})?)\s*(?:м²|м2|кв\.\s*м)(?![\p{L}\d])"""))
            ?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in 5.0..5000.0 } ?: 0.0
        val rooms = unique(Regex("""(?i)(?<!\d)([1-9])\s*[-–]?\s*(?:к\.\s*квартир|комнатн\p{L}*\s+квартир)"""))
            ?.toIntOrNull() ?: 0
        val floorPattern = Regex("""(?i)(?<!\d)(\d{1,3})\s*/\s*(\d{1,3})\s*(?:эт\.?|этаж)""")
        val floor = unique(floorPattern)?.toIntOrNull() ?: 0
        val total = unique(floorPattern, 2)?.toIntOrNull() ?: 0
        return SharedFacts(price, area, rooms, floor.takeIf { it in 1..total && total <= 200 } ?: 0,
            total.takeIf { floor in 1..it && it <= 200 } ?: 0)
    }

    suspend fun fromShare(db: AppDb, input: String): Result = db.withTransaction {
        val original = requireNotNull(sharedLink(input)) { "Поделитесь прямой HTTPS-ссылкой на карточку объекта" }
        val known = db.hunter().allOffers().firstOrNull {
            ListingOrigin.original(it)?.identity == original.identity && GeoScope.eligible(it) && AutoRules.valid(it)
        }
        if (known != null) return@withTransaction fromOffer(db, known.id)
        val existing = db.dao().allProperties().firstOrNull {
            ListingOrigin.original(Offer(source = "", url = it.url))?.identity == original.identity
        }
        val facts = sharedFacts(input)
        val property = existing ?: Property(title = "Объявление №${original.id}", url = original.url, status = "review",
            price = facts.price, area = facts.area, rooms = facts.rooms, floor = facts.floor)
        save(db, property, null, if (existing == null) facts.floorsTotal else 0)
    }

    suspend fun fromOffer(db: AppDb, id: Long): Result = db.withTransaction {
        val offer = requireNotNull(db.hunter().offer(id)) { "Объявление отсутствует в базе" }
        check(GeoScope.eligible(offer) && AutoRules.valid(offer)) { "Объявление больше недоступно" }
        val dao = db.dao()
        val identity = ListingOrigin.original(offer)?.identity
        val previous = dao.allProperties().firstOrNull {
            AutoRules.isDuplicate(offer, it) ||
                (identity != null && ListingOrigin.original(Offer(source = "", url = it.url))?.identity == identity)
        }
        // ownerPhone/ownerName are the feed's explicit CRM-permission fields.
        val phone = ListingOrigin.dialable(offer.ownerPhone)
        val owner = if (offer.seller == "owner" && phone != null && !offer.ownerName.isNullOrBlank()) {
            val key = OfflineLogic.phoneKey(phone)
            dao.allClients().firstOrNull { it.role == "owner" && OfflineLogic.phoneKey(it.phone) == key }?.id
                ?: dao.saveClient(Client(name = offer.ownerName, phone = phone, role = "owner", notes = "Источник: ${offer.source}"))
        } else null
        val facts = AutoRules.propertyFromOffer(offer, owner)
        val property = when {
            previous == null -> facts
            previous.status == "review" -> facts.copy(id = previous.id, createdAt = previous.createdAt, ownerContactBlocked = previous.ownerContactBlocked, ownerId = owner ?: previous.ownerId)
            else -> previous.copy(sourceOfferId = previous.sourceOfferId ?: id, ownerId = previous.ownerId ?: owner)
        }
        save(db, property, offer)
    }

    private suspend fun save(db: AppDb, property: Property, offer: Offer?, sharedFloors: Int = 0): Result {
        val propertyId = db.dao().saveProperty(property)
        val saved = property.copy(id = propertyId)
        val pubs = db.publications()
        val existing = pubs.listingForProperty(propertyId)
        val base = Listing(propertyId = propertyId, address = saved.address, price = saved.price, area = saved.area,
            rooms = saved.rooms, floor = saved.floor, floorsTotal = offer?.floorsTotal ?: sharedFloors, market = saved.type,
            title = saved.title.take(50))
        val listing = if (existing == null || (existing.approvedAt == null && existing.address.isBlank() && saved.address.isNotBlank())) {
            val draft = if (existing == null) base.copy(description = SmartAi.factsText(base).trim())
            else existing.copy(address = base.address, price = existing.price.takeIf { it > 0 } ?: base.price,
                area = existing.area.takeIf { it > 0 } ?: base.area, rooms = existing.rooms.takeIf { it > 0 } ?: base.rooms,
                floor = existing.floor.takeIf { it > 0 } ?: base.floor, floorsTotal = existing.floorsTotal.takeIf { it > 0 } ?: base.floorsTotal,
                title = base.title, description = SmartAi.factsText(base).trim(), market = base.market)
            pubs.saveListing(draft)
        } else existing.id
        if (saved.status == "active") ru.vladis.realtorai.owner.OwnerService.begin(db, propertyId)
        val buyers = db.dao().allClients().count { AutoRules.matches(it, listOf(saved)).isNotEmpty() }
        return Result(listing, propertyId, buyers, saved.status == "review")
    }
}
