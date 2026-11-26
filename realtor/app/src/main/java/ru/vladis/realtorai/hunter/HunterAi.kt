package ru.vladis.realtorai.hunter

import android.content.Context
import ru.vladis.realtorai.ai.Groq
import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.data.Store

/**
 * Экономия Groq: запросы только по нажатию кнопки; локальный предотбор (≤15 объектов, по одной строке);
 * в промпт идут только факты из базы; ответы кэшируются по хешу промпта на время жизни процесса;
 * короткие инструкции и лимит слов. Без ключа доступны локальные результаты.
 */
object HunterAi {
    private val cache = HashMap<Int, String>()

    private suspend fun ask(ctx: Context, system: String, user: String): String {
        if (ru.vladis.realtorai.ai.DeviceModel.installed(ctx))
            return ru.vladis.realtorai.ai.DeviceModel.answer(ctx, system, listOf("user" to user))
        val key = (system + user).hashCode()
        cache[key]?.let { return it }
        val st = Store(ctx); if (st.apiKey.isBlank()) return "Локальная сводка (без языковой модели):\n" + user + "\n\nПроверьте детали и согласуйте следующий шаг."
        return Groq.chat(st.apiKey, st.model, system, listOf("user" to user)).also { cache[key] = it }
    }

    fun line(d: Deal): String { val o = d.o
        return "#${o.id} | ${o.rooms}к | ${o.area} м² | эт ${o.floor}/${o.floorsTotal} | ${o.district.ifBlank { o.address.take(30) }} | ${o.price} ₽" +
            (d.discount?.let { " | ${(it * 100).toInt()}% к оценке рынка" } ?: "") }

    suspend fun match(ctx: Context, c: Client, picks: List<Deal>): String = ask(ctx,
        "Ты помощник риелтора. Выбери до 5 вариантов из списка под клиента СТРОГО по данным списка и пожеланиям. Для каждого: #id, одно предложение «почему подходит», один возможный минус. " +
            "Не выдумывай метро, ремонт, состояние дома. Если ничего не подходит — скажи. До 150 слов.",
        "Клиент: бюджет ${c.budget} ₽. Пожелания: ${c.notes.take(400).ifBlank { "не указаны" }}\nОбъекты:\n" + picks.joinToString("\n") { line(it) })

    suspend fun negotiate(ctx: Context, facts: List<String>): String = ask(ctx,
        "Подготовь аргументы для торга с собственником: 4–6 коротких пунктов ТОЛЬКО на основе фактов ниже, вежливо, без давления и выдуманных данных. " +
            "В конце — одна фраза-открытие для разговора. До 150 слов.", facts.joinToString("\n") { "- $it" })
}
