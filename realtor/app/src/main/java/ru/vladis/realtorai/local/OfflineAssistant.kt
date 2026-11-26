package ru.vladis.realtorai.local

import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.data.Property

object OfflineAssistant {
    fun answer(mode: String, text: String, p: Property?, c: Client?, properties: List<Property>): String {
        val name = c?.name?.takeIf { it.isNotBlank() } ?: ""
        val subject = p?.title?.takeIf { it.isNotBlank() } ?: "объект недвижимости"
        val prefix = "Локальный шаблон • проверьте перед использованием\n\n"
        return prefix + when(mode) {
            "Подбор" -> {
                val budget = c?.budget ?: 0
                val candidates = properties.filter { it.status == "active" && it.price > 0 && (budget <= 0 || it.price <= budget) }.sortedBy { it.price }.take(5)
                if (candidates.isEmpty()) "В базе нет подходящих объектов. Уточните запрос или импортируйте объявления." else "Отбор по известному бюджету (остальные требования проверьте вручную):\n" + candidates.joinToString("\n") { "• ${it.title} — ${it.price} ₽, ${it.address}" }
            }
            "Скрипт звонка" -> "Здравствуйте${if (name.isNotBlank()) ", $name" else ""}! Удобно говорить? Хотел уточнить ваш запрос по недвижимости. Какой бюджет, район и срок сделки? Что для вас принципиально? Предлагаю согласовать следующий шаг.\n\nЗапрос: $text"
            "Сообщение" -> "Здравствуйте${if (name.isNotBlank()) ", $name" else ""}! Хотел обсудить $subject и уточнить актуальность вашего запроса. Когда вам удобно созвониться?\n\nКонтекст для редактирования: $text"
            "Возражения" -> OfflineLogic.objection(text)
            else -> buildString {
                if (p != null) { appendLine("Объект: ${p.title}"); appendLine("Цена: ${p.price} ₽ · площадь: ${p.area} м²"); if (p.area > 0 && p.price > 0) appendLine("Цена за м²: ${(p.price / p.area).toLong()} ₽") }
                appendLine("Проверьте собственника, документы, ограничения, фактическое состояние и сопоставимые предложения. Без проверенных аналогов рыночную стоимость определить нельзя."); append("Вопрос: $text")
            }
        }
    }
}
