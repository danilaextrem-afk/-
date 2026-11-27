package ru.vladis.realtorai.ai

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import ru.vladis.realtorai.data.Store
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CloudQuota(
    val usedTokens: Int?,
    val remainingTokens: Long?,
    val remainingRequests: Long?,
    val retryAfter: String?
)

data class CloudReply(val text: String, val quota: CloudQuota)

object Groq {
    const val PRIMARY = "openai/gpt-oss-120b"
    private val http = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun quota(headers: Headers, used: Int? = null) = CloudQuota(
        used,
        headers["x-ratelimit-remaining-tokens"]?.toLongOrNull(),
        headers["x-ratelimit-remaining-requests"]?.toLongOrNull(),
        headers["retry-after"]?.takeIf { it.all(Char::isDigit) }
    )

    suspend fun chat(key: String, model: String, system: String, history: List<Pair<String, String>>): String =
        complete(key, system, history, model = model).text

    suspend fun complete(
        key: String,
        system: String,
        history: List<Pair<String, String>>,
        stream: Boolean = false,
        model: String = PRIMARY,
        onPartial: (String) -> Unit = {}
    ): CloudReply = suspendCancellableCoroutine { continuation ->
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        history.forEach { (role, text) ->
            messages.put(JSONObject().put("role", role).put("content", text))
        }
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("stream", stream)
            .apply { if (stream) put("stream_options", JSONObject().put("include_usage", true)) }
            .put("temperature", 0.4)
            .apply { if (model == PRIMARY) put("reasoning_effort", "low").put("include_reasoning", false) }
            .put("max_completion_tokens", 4096)
            .toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $key").post(body).build()
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(IOException("Не удалось связаться с Groq. Проверьте подключение."))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching { response.use { read(it, stream, onPartial) } }
                continuation.resumeWith(result)
            }
        })
    }

    internal fun read(response: Response, stream: Boolean, onPartial: (String) -> Unit): CloudReply {
        if (!response.isSuccessful) error(when (response.code) {
            401, 403 -> "Groq отклонил ключ или доступ к модели"
            429 -> quota(response.headers).retryAfter?.let { "Квота Groq исчерпана. Повторите через $it сек." }
                ?: "Квота Groq исчерпана. Повторите позже или выберите локальный режим."
            413 -> "Запрос слишком велик для Groq. Начните новый диалог или сократите вложенный текст."
            in 500..599 -> "Groq временно недоступен. Повторите позже."
            else -> "Groq отклонил запрос (HTTP ${response.code})"
        })
        val body = checkNotNull(response.body) { "Groq вернул пустой ответ" }
        if (!stream) {
            val packet = JSONObject(body.string())
            return CloudReply(
                packet.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"),
                quota(response.headers, usedTokens(packet))
            )
        }
        val output = StringBuilder()
        var tokens: Int? = null
        var finished = false
        val source = body.source()
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (!line.startsWith("data:")) continue
            val data = line.substringAfter(':').trim()
            if (data == "[DONE]") {
                finished = true
                break
            }
            val packet = JSONObject(data)
            check(!packet.has("error")) { "Groq прервал генерацию" }
            tokens = usedTokens(packet) ?: tokens
            val delta = packet.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
            if (delta == null || delta.isNull("content")) continue
            val text = delta.optString("content")
            if (text.isNotEmpty()) {
                output.append(text)
                onPartial(output.toString())
            }
        }
        check(finished) { "Связь с Groq прервалась до конца ответа" }
        return CloudReply(output.toString(), quota(response.headers, tokens))
    }

    private fun usedTokens(packet: JSONObject): Int? =
        (packet.optJSONObject("usage") ?: packet.optJSONObject("x_groq")?.optJSONObject("usage"))
            ?.optInt("total_tokens", -1)?.takeIf { it >= 0 }
}

/** Cloud requests exclude CRM attachments and local conversation history. */
object CloudAi {
    private val contactPattern = Regex("""[\p{L}\d._%+-]+@[\p{L}\d.-]+\.[\p{L}]{2,}|(?<!\d)(?:\+7|8)[\s().-]*(?:\d[\s().-]*){10}(?!\d)""")
    fun containsContact(text: String) = contactPattern.containsMatchIn(text)
    fun referencesClients(text: String, clients: List<ru.vladis.realtorai.data.Client>): Boolean {
        fun normalize(value: String) = value.lowercase().replace('ё', 'е').replace(Regex("[^\\p{L}]+"), " ").trim()
        val query = " ${normalize(text)} "
        return clients.any { client ->
            val name = normalize(client.name)
            name.length >= 3 && " $name " in query
        }
    }

    fun enabled(ctx: Context): Boolean = Store(ctx).let { it.cloudEnabled && it.cloudKey.isNotBlank() }

    suspend fun answer(ctx: Context, system: String, prompt: String): String =
        complete(ctx, system, listOf("user" to prompt), stream = false).text

    suspend fun complete(
        ctx: Context,
        system: String,
        history: List<Pair<String, String>>,
        stream: Boolean = true,
        onPartial: (String) -> Unit = {}
    ): CloudReply {
        val store = Store(ctx)
        val key = store.cloudKey
        check(store.cloudEnabled && key.isNotBlank()) { "Облачный ИИ не подключён" }
        check(history.none { containsContact(it.second) }) { "Уберите телефон или почту из запроса либо выберите локальный режим" }
        return Groq.complete(key, system, history, stream, onPartial = onPartial).also {
            check(it.text.isNotBlank()) { "Groq вернул пустой ответ" }
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
