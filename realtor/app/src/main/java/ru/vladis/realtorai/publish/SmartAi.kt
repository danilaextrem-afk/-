package ru.vladis.realtorai.publish

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import ru.vladis.realtorai.ai.Groq
import ru.vladis.realtorai.data.Store
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import kotlin.math.max

object Img {
    /** TODO: учесть EXIF-ориентацию (androidx.exifinterface) */
    fun jpeg(path: String, maxSide: Int, q: Int = 80): ByteArray {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var s = 1
        while (max(o.outWidth, o.outHeight) / s > maxSide * 2) s *= 2
        val b = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s }) ?: error("Не удалось прочитать фото")
        val k = maxSide.toFloat() / max(b.width, b.height)
        val sc = if (k < 1f) Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true) else b
        return try { ByteArrayOutputStream().also { check(sc.compress(Bitmap.CompressFormat.JPEG, q, it)) }.toByteArray() }
        finally { if (sc !== b) sc.recycle(); b.recycle() }
    }
}

object SmartAi {
    const val DEFAULT_VISION = "meta-llama/llama-4-scout-17b-16e-instruct" // проверьте актуальную vision-модель в консоли Groq
    data class Draft(val cover: String, val title: String, val description: String, val features: List<String>)

    fun factsText(l: Listing) = buildString {
        appendLine("Адрес: ${l.address}"); appendLine("Рынок: ${if (l.market == "primary") "первичка (новостройка)" else "вторичка"}")
        if (l.rooms > 0) appendLine("Комнат: ${l.rooms}"); if (l.area > 0) appendLine("Площадь: ${l.area} м²")
        if (l.kitchenArea > 0) appendLine("Кухня: ${l.kitchenArea} м²")
        if (l.floor > 0) appendLine("Этаж: ${l.floor}" + if (l.floorsTotal > 0) " из ${l.floorsTotal}" else "")
        if (l.renovation.isNotBlank()) appendLine("Ремонт (со слов агента): ${l.renovation}")
        if (l.extra.isNotBlank()) appendLine("Дополнительно: ${l.extra}")
        if (l.features.isNotBlank()) appendLine("Видно на фото: ${l.features.lines().joinToString(", ")}")
    }

    private fun json(s: String) = JSONObject(s.substring(s.indexOf('{'), s.lastIndexOf('}') + 1))

    private suspend fun vision(key: String, model: String, images: List<ByteArray>, prompt: String): String = withContext(Dispatchers.IO) {
        val content = JSONArray().put(JSONObject().put("type", "text").put("text", prompt))
        images.forEach {
            content.put(JSONObject().put("type", "image_url").put("image_url",
                JSONObject().put("url", "data:image/jpeg;base64," + Base64.encodeToString(it, Base64.NO_WRAP))))
        }
        val body = JSONObject().put("model", model).put("temperature", 0.2)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
            .toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $key").post(body).build()
        Http.c.newCall(req).execute().use { r ->
            val s = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("Groq ${r.code}: ${s.take(200)}")
            JSONObject(s).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    /** 1) vision: тип/качество фото, обложка, видимые особенности; 2) текст: заголовок + описание строго по фактам. */
    suspend fun run(ctx: Context, l: Listing, visionModel: String): Draft {
        val store = Store(ctx); val key = store.apiKey
        if (key.isBlank()) return withContext(Dispatchers.IO) {
            val cover = l.photoList().take(10).mapNotNull { path ->
                runCatching {
                    val bytes = Img.jpeg(path, 256)
                    val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Фото")
                    try {
                        var sum = 0.0; var edges = 0.0; var n = 0
                        for (y in 1 until b.height step 4) for (x in 1 until b.width step 4) {
                            fun light(c: Int) = (android.graphics.Color.red(c) + android.graphics.Color.green(c) + android.graphics.Color.blue(c)) / 3.0
                            val v = light(b.getPixel(x, y)); sum += v; edges += kotlin.math.abs(v - light(b.getPixel(x-1,y))); n++
                        }
                        check(n > 0); path to (100 - kotlin.math.abs(sum/n - 135) + edges/n)
                    } finally { b.recycle() }
                }.getOrNull()
            }.maxByOrNull { it.second }?.first ?: l.cover
            val title = (if (l.rooms > 0) "${l.rooms}-комнатная квартира" else "Квартира") + (if (l.area > 0) ", ${l.area} м²" else "")
            Draft(cover, title.take(50), factsText(l) + "\nСвяжитесь для уточнения деталей и организации просмотра.", l.features.lines().filter { it.isNotBlank() })
        }
        val photos = l.photoList().take(10)
        var cover = l.cover; val feats = linkedSetOf<String>()
        if (photos.isNotEmpty()) {
            var best = -1; var bestScore = -1.0
            photos.chunked(5).forEachIndexed { ci, chunk ->
                val imgs = withContext(Dispatchers.IO) { chunk.map { Img.jpeg(it, 896, 70) } }
                val ans = vision(key, visionModel.ifBlank { DEFAULT_VISION }, imgs,
                    "Это фото квартиры для объявления (${chunk.size} шт., индексы 0..${chunk.size - 1}). Для каждого определи room " +
                    "(кухня|комната|ванная|коридор|фасад|планировка|другое), quality 1-5 и cover_score 0-10 (пригодность для обложки: светлая, чёткая жилая комната или кухня; " +
                    "ванная, планировка, тёмные/размытые — низко). visible_features — ТОЛЬКО то, что чётко видно (например «паркет», «панорамные окна»), без догадок. " +
                    "Верни только JSON: {\"photos\":[{\"i\":0,\"room\":\"\",\"quality\":3,\"cover_score\":5}],\"visible_features\":[\"\"]}")
                val j = json(ans); val arr = j.getJSONArray("photos")
                for (k in 0 until arr.length()) {
                    val o = arr.getJSONObject(k); val g = ci * 5 + o.getInt("i")
                    val sc = o.optDouble("cover_score", 0.0) + o.optDouble("quality", 0.0) / 10
                    if (o.getInt("i") in chunk.indices && g in photos.indices && sc.isFinite() && sc > bestScore) { bestScore = sc; best = g }
                }
                j.optJSONArray("visible_features")?.let { f -> for (k in 0 until f.length()) f.getString(k).takeIf { it.isNotBlank() }?.let(feats::add) }
            }
            if (best >= 0) cover = photos[best]
        }
        val facts = factsText(l.copy(features = feats.joinToString("\n")))
        val ans = Groq.chat(key, store.model,
            "Ты копирайтер объявлений о продаже квартиры. Используй ТОЛЬКО факты из блока «Факты». Запрещено выдумывать: метро, расстояния, состояние, ремонт, документы, " +
            "год постройки, материал дома, преимущества района, юридическую чистоту. Не указывай в описании цену, адрес, телефон и числа, которых нет в фактах. " +
            "Нет данных — просто не упоминай. Без капса и клише «уникальное предложение». Заголовок ≤ 50 символов; описание 500–900 символов, абзацы, спокойный деловой тон. " +
            "Верни только JSON: {\"title\":\"\",\"description\":\"\"}.",
            listOf("user" to "Факты:\n$facts"))
        val j = json(ans)
        return Draft(cover, j.getString("title").take(50).trim(), j.getString("description").trim(), feats.toList())
    }

    private fun nums(s: String) = Regex("\\d+(?:[.,]\\d+)?").findAll(s).map { BigDecimal(it.value.replace(',', '.')).stripTrailingZeros().toPlainString() }.toSet()

    /** Проверка текста на неподтверждённые числа и утверждения. */
    fun check(l: Listing): List<String> {
        val facts = factsText(l).lowercase(); val allowed = nums(facts); val d = l.description.lowercase()
        val w = mutableListOf<String>()
        nums(l.description + " " + l.title).filter { it !in allowed }.forEach { w += "В тексте число «$it», которого нет в данных объекта" }
        listOf("евроремонт", "дизайнерск", "метро", "ипотек", "торг", "чистая продажа", "свободная продажа", "тихий", "экологич")
            .filter { d.contains(it) && !facts.contains(it) }.forEach { w += "Проверьте утверждение «$it» — его нет в данных" }
        return w
    }
}
