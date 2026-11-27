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

    suspend fun fromShare(db: AppDb, input: String): Result = db.withTransaction {
        val original = requireNotNull(sharedLink(input)) { "Поделитесь прямой HTTPS-ссылкой на карточку объекта" }
        val known = db.hunter().allOffers().firstOrNull {
            ListingOrigin.original(it)?.identity == original.identity && GeoScope.eligible(it) && AutoRules.valid(it)
        }
        if (known != null) return@withTransaction fromOffer(db, known.id)
        val existing = db.dao().allProperties().firstOrNull {
            ListingOrigin.original(Offer(source = "", url = it.url))?.identity == original.identity
        }
        val property = existing ?: Property(title = "Объявление №${original.id}", url = original.url, status = "review")
        save(db, property, null)
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
            previous.status == "review" -> facts.copy(id = previous.id, createdAt = previous.createdAt)
            else -> previous.copy(sourceOfferId = previous.sourceOfferId ?: id, ownerId = previous.ownerId ?: owner)
        }
        save(db, property, offer)
    }

    private suspend fun save(db: AppDb, property: Property, offer: Offer?): Result {
        val propertyId = db.dao().saveProperty(property)
        val saved = property.copy(id = propertyId)
        val pubs = db.publications()
        val existing = pubs.listingForProperty(propertyId)
        val base = Listing(propertyId = propertyId, address = saved.address, price = saved.price, area = saved.area,
            rooms = saved.rooms, floor = saved.floor, floorsTotal = offer?.floorsTotal ?: 0, market = saved.type,
            title = saved.title.take(50))
        val listing = if (existing == null || (existing.approvedAt == null && existing.address.isBlank() && saved.address.isNotBlank())) {
            val draft = if (existing == null) base.copy(description = SmartAi.factsText(base).trim())
            else existing.copy(address = base.address, price = existing.price.takeIf { it > 0 } ?: base.price,
                area = existing.area.takeIf { it > 0 } ?: base.area, rooms = existing.rooms.takeIf { it > 0 } ?: base.rooms,
                floor = existing.floor.takeIf { it > 0 } ?: base.floor, floorsTotal = existing.floorsTotal.takeIf { it > 0 } ?: base.floorsTotal,
                title = base.title, description = SmartAi.factsText(base).trim(), market = base.market)
            pubs.saveListing(draft)
        } else existing.id
        val buyers = db.dao().allClients().count { AutoRules.matches(it, listOf(saved)).isNotEmpty() }
        return Result(listing, propertyId, buyers, saved.status == "review")
    }
}
