package ru.vladis.realtorai.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.vladis.realtorai.data.Property

/** Ручной импорт: страница по ссылке (best effort) или вставленный текст. Парсинг regex + опционально ИИ. */
object Importer {
    private val http = OkHttpClient()

    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Android)").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            r.body!!.string()
                .replace(Regex("(?s)<(script|style).*?</\\1>"), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("&nbsp;"), " ")
                .replace(Regex("\\s+"), " ").take(8000)
        }
    }

    fun parse(text: String, url: String = ""): Property {
        val t = text.replace('\u00a0', ' ')
        val mln = Regex("""(\d+(?:[.,]\d+)?)\s*млн""").find(t)
        val price = mln?.let { (it.groupValues[1].replace(',', '.').toDouble() * 1_000_000).toLong() }
            ?: Regex("""(\d{1,3}(?: \d{3}){1,3})\s*(?:₽|руб)""").find(t)?.groupValues?.get(1)?.replace(" ", "")?.toLongOrNull() ?: 0L
        val area = Regex("""(\d+(?:[.,]\d+)?)\s*(?:м²|м2|кв\.?\s*м)""").find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        val rooms = Regex("""(\d)\s*-?\s*(?:к(?![а-яё])|комн)""").find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val floor = Regex("""(\d{1,2})\s*/\s*\d{1,2}\s*эт""").find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val title = t.trim().lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Объект"
        return Property(title = title, price = price, area = area, rooms = rooms, floor = floor,
            url = url, description = t.take(1500))
    }

    suspend fun aiRefine(key: String, model: String, text: String, base: Property): Property {
        val ans = Groq.chat(key, model,
            "Извлеки из объявления данные. Верни ТОЛЬКО JSON: {\"title\":str,\"address\":str,\"price\":int рублей,\"area\":number м²,\"rooms\":int,\"floor\":int,\"type\":\"primary\"|\"secondary\"}. Неизвестное — 0 или \"\".",
            listOf("user" to text.take(6000)))
        val j = JSONObject(ans.substring(ans.indexOf('{'), ans.lastIndexOf('}') + 1))
        return base.copy(
            title = j.optString("title").ifBlank { base.title },
            address = j.optString("address"),
            price = j.optLong("price").takeIf { it > 0 } ?: base.price,
            area = j.optDouble("area", 0.0).takeIf { it > 0 } ?: base.area,
            rooms = j.optInt("rooms").takeIf { it > 0 } ?: base.rooms,
            floor = j.optInt("floor").takeIf { it > 0 } ?: base.floor,
            type = if (j.optString("type") == "primary") "primary" else "secondary")
    }
}
