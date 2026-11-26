package ru.vladis.realtorai.local

import kotlin.math.pow
import kotlin.math.roundToLong

/** Deterministic, offline tools; no language model or external API. */
object OfflineLogic {
    fun mortgage(principal: Long, annualRate: Double, months: Int): Long? {
        if (principal < 0 || !annualRate.isFinite() || annualRate !in 0.0..100.0 || months !in 1..600) return null
        val r = annualRate / 1200
        val payment = if (r == 0.0) principal.toDouble() / months else principal * r / (1 - (1 + r).pow(-months))
        return payment.takeIf { it.isFinite() && it <= Long.MAX_VALUE.toDouble() }?.roundToLong()
    }
    fun priority(stage: String, budget: Long, hasPhone: Boolean): Int = ((when(stage) {
        "deposit" -> 75; "showing" -> 60; "contact" -> 45; "new" -> 30; else -> 0
    }) + (if (budget > 0) 15 else 0) + (if (hasPhone) 10 else 0)).coerceAtMost(100)
    fun phoneKey(phone: String): String = phone.filter(Char::isDigit).let { if (it.length == 11 && it.startsWith("8")) "7" + it.drop(1) else it }
    fun probability(stage: String) = when(stage) { "new" -> .10; "contact" -> .25; "showing" -> .45; "deposit" -> .80; else -> 0.0 }
    fun objection(text: String): String = when {
        text.contains("дорог", true) || text.contains("цен", true) -> "Понимаю, что цена важна. С каким бюджетом и какими аналогами сравниваете? Предлагаю проверить сопоставимые объекты и обсудить варианты."
        text.contains("дум", true) -> "Конечно. Какие вопросы остались открытыми? Могу подготовить ответы и договориться о времени следующего контакта."
        text.contains("комисс", true) -> "Давайте уточним условия комиссии и перечень услуг. Перед принятием решения я покажу расчёт и зафиксируем договорённости."
        else -> "Понимаю. Уточните, пожалуйста, что именно вызывает сомнения. Проверим факты и подберём решение без давления."
    }
}
