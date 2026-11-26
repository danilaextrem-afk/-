package ru.vladis.realtorai.owner

import ru.vladis.realtorai.data.Property
import ru.vladis.realtorai.hunter.Dedup
import ru.vladis.realtorai.hunter.Offer
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToLong

enum class OwnerStep(val title: String, val checklist: String) {
    CALL("Первый звонок", "Уточнить условия продажи, ожидания и удобное время связи"),
    AGREEMENT("Сотрудничество", "Подтвердить условия, подготовить SMS или назначить встречу"),
    ACTIVATION("Активация", "Согласовать адрес, характеристики и начальную цену"),
    MODERATION("Модерация", "Проверить фактические статусы в Smart Publish и кабинетах площадок"),
    PACKAGING("Упаковка", "Проверить фото, описание, планировку и ссылку на реальный 3D-тур, если он есть"),
    FEEDBACK("Обратная связь", "Уточнить у собственника входящие звонки и впечатления"),
    WEEK_ONE("Первая неделя", "Сверить рекламу, обращения, звонки и показы"),
    REPORTING("Регулярные отчёты", "Подготовить, проверить и передать собственнику отчёт"),
    PRICING("Корректировка стоимости", "Сравнить аналоги и согласовать изменение цены"),
    CLOSING("Сопровождение сделки", "Согласовать документы, расчёты и передачу объекта"),
    FINISHED("Сделка завершена", "Журнал сохранён")
}

data class PriceStrategy(val fast: Double = .95, val normal: Double = 1.0, val patient: Double = 1.05) {
    fun valid() = listOf(fast, normal, patient).all { it.isFinite() && it in .5..1.5 } && fast <= normal && normal <= patient
}

object OwnerRules {
    const val day = 86_400_000L

    fun next(step: OwnerStep): OwnerStep = OwnerStep.entries.getOrElse(step.ordinal + 1) { step }

    fun reportDue(reference: Long, weeksReports: Int, zone: ZoneId): Long =
        Instant.ofEpochMilli(reference).atZone(zone).plusDays(if (weeksReports == 2) 4 else 7).toInstant().toEpochMilli()

    fun metric(value: Int?): String = value?.toString() ?: "нет данных"

    fun comparisons(property: Property, offers: List<Offer>, location: String, now: Long): List<Offer> {
        val region = ru.vladis.realtorai.hunter.GeoScope.region(property) ?: return emptyList()
        val district = location.trim()
        if (district.length < 3 || property.area <= 0 || !property.area.isFinite() || property.rooms <= 0) return emptyList()
        val address = Dedup.norm(property.address)
        val original = Offer(source = "crm", address = property.address, rooms = property.rooms, floor = property.floor, area = property.area)
        val ownUrl = ru.vladis.realtorai.autopilot.AutoRules.urlKey(property.url)
        val candidates = offers.filter {
            val url = ru.vladis.realtorai.autopilot.AutoRules.urlKey(it.url)
            ru.vladis.realtorai.hunter.GeoScope.eligible(it, now) && ru.vladis.realtorai.hunter.GeoScope.region(it) == region && it.deal == "sale" && it.market == property.type &&
                !it.source.startsWith("Торги.Открыто", ignoreCase = true) &&
                !property.housingKind.isNullOrBlank() && it.housingKind == property.housingKind &&
                it.rooms == property.rooms && it.price > 0 && it.area.isFinite() &&
                abs(it.area - property.area) <= property.area * .20 &&
                it.address.isNotBlank() && (it.district + " " + it.address).contains(district, ignoreCase = true) &&
                it.lastSeen in (now - 30 * day)..now && url != null && url != ownUrl &&
                (address.isBlank() || !Dedup.same(original, it))
        }.sortedWith(compareBy<Offer> { it.price }.thenByDescending { it.lastSeen })
        val selected = mutableListOf<Offer>()
        for (offer in candidates) {
            if (selected.none { Dedup.same(it, offer) || ru.vladis.realtorai.autopilot.AutoRules.urlKey(it.url) ==
                    ru.vladis.realtorai.autopilot.AutoRules.urlKey(offer.url) }) selected += offer
            if (selected.size == 5) break
        }
        return selected
    }

    fun estimate(area: Double, comparable: List<Offer>, strategy: PriceStrategy): List<Long> {
        if (comparable.size < 3 || !area.isFinite() || area <= 0 || !strategy.valid()) return emptyList()
        val average = comparable.map { it.price / it.area }.average()
        if (!average.isFinite() || average <= 0) return emptyList()
        return listOf(strategy.fast, strategy.normal, strategy.patient).map { (average * area * it).roundToLong() }
    }
}
