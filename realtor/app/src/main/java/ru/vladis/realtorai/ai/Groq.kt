package ru.vladis.realtorai.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Groq {
    private val http = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()

    suspend fun chat(key: String, model: String, system: String, history: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", system))
            history.forEach { msgs.put(JSONObject().put("role", it.first).put("content", it.second)) }
            val body = JSONObject().put("model", model).put("messages", msgs).put("temperature", 0.4)
                .toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $key").post(body).build()
            http.newCall(req).execute().use { r ->
                val s = r.body?.string().orEmpty()
                if (!r.isSuccessful) error("Groq ${r.code}: ${s.take(200)}")
                JSONObject(s).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            }
        }
}

object Prompts {
    const val BASE = "Ты ИИ-ассистент риелтора компании «Владис» (Москва, первичная и вторичная недвижимость). " +
        "Отвечай по-русски, кратко и практично. Не выдумывай факты об объектах и рынке: если данных нет — скажи об этом."
    val modes: List<Pair<String, String>> = listOf(
        "Анализ" to "Проанализируй объявление: плюсы, риски, вопросы к собственнику, что проверить в документах, оценка адекватности цены по приведённым данным.",
        "Скрипт звонка" to "Составь короткий скрипт телефонного звонка с вариантами ответов клиента.",
        "Сообщение" to "Напиши вежливое сообщение собственнику (мессенджер/SMS), 3–6 предложений, с чётким призывом к действию.",
        "Возражения" to "Предложи 3 варианта отработки возражения клиента: короткий, аргументированный, с вопросом.",
        "Подбор" to "Подбери подходящие варианты ТОЛЬКО из списка объектов ниже под запрос клиента и объясни выбор."
    )
}
