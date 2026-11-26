package ru.vladis.realtorai.local

/** Explicitly structured imports and deterministic conversation aids. No network. */
object SalesTools {
    data class Lead(val name: String, val phone: String, val budget: Long, val owner: Boolean)
    data class LeadImport(val rows: List<Lead>, val rejected: Int, val duplicates: Int)
    fun importLeads(text: String, existingPhones: Collection<String>): LeadImport {
        val seen = existingPhones.map(OfflineLogic::phoneKey).filter { it.isNotBlank() }.toMutableSet()
        val rows = mutableListOf<Lead>(); var bad = 0; var dup = 0
        text.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val p = line.split(';').map(String::trim)
            if (p.size != 4 || p[0].isBlank()) { bad++; return@forEach }
            val phone = OfflineLogic.phoneKey(p[1]); val budget = p[2].replace(" ", "").toLongOrNull()
            val role = p[3].lowercase()
            if (phone.length !in 7..15 || budget == null || budget < 0 || role !in setOf("покупатель", "собственник")) { bad++; return@forEach }
            if (!seen.add(phone)) { dup++; return@forEach }
            rows += Lead(p[0], p[1], budget, role == "собственник")
        }
        return LeadImport(rows, bad, dup)
    }
    fun conversation(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val cues = listOf("дорог" to "Уточнить бюджет и сопоставимые предложения", "ипотек" to "Уточнить взнос, ставку и срок", "показ" to "Согласовать дату и время показа", "документ" to "Уточнить перечень документов", "дум" to "Уточнить открытые вопросы и дату повторного контакта")
        return cues.filter { text.contains(it.first, true) }.map { it.second }.ifEmpty { listOf("Уточнить потребность, бюджет и следующий шаг") }
    }
    fun proposal(client: String, title: String, address: String, price: Long, area: Double, rooms: Int): String = buildString {
        appendLine("Предложение для ${client.ifBlank { "клиента" }}")
        appendLine(title.ifBlank { "Объект недвижимости" })
        if (address.isNotBlank()) appendLine("Адрес: $address")
        if (price > 0) appendLine("Цена: $price ₽")
        if (area.isFinite() && area > 0) appendLine("Площадь: $area м²")
        if (rooms > 0) appendLine("Комнат: $rooms")
        append("Условия и актуальность уточняются перед просмотром.")
    }
}
