package ru.vladis.realtorai.publish

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Avito/Cian/Domclick/Yandex/VT24 — через XML-фиды/экспорт (см. Feeds). Прямые API — только Telegram и VK. */
enum class Platform(val label: String, val isApi: Boolean) {
    AVITO("Авито", false), CIAN("Циан", false), DOMCLICK("Домклик", false), YANDEX("Яндекс Недвижимость", false),
    TELEGRAM("Telegram", true), VK("ВКонтакте", true), VT24("VT 24 (Владис)", false)
}

data class PubResult(val status: String, val externalId: String = "", val meta: String = "", val url: String = "", val message: String = "")

/** Реализации бросают исключение при любой ошибке; успех = подтверждённый ответ API. */
interface Publisher {
    suspend fun publish(l: Listing): PubResult
    suspend fun updatePrice(l: Listing, p: Publication): PubResult
    suspend fun unpublish(l: Listing, p: Publication): PubResult
}

object Http { val c: OkHttpClient = OkHttpClient.Builder().callTimeout(90, TimeUnit.SECONDS).build() }

private fun send(req: Request): String = Http.c.newCall(req).execute().use {
    val body = it.body?.string() ?: error("Пустой ответ API")
    if (!it.isSuccessful) {
        val details = runCatching { JSONObject(body).optString("description") }.getOrDefault("")
        error("HTTP ${it.code}" + if (details.isNotBlank()) ": $details" else "")
    }
    body
}
private fun metaMap(m: String) = m.split(';').filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }

class TelegramPublisher(private val token: String, private val chat: String) : Publisher {
    private fun call(method: String, body: RequestBody): JSONObject {
        val raw = send(Request.Builder().url("https://api.telegram.org/bot$token/$method").post(body).build())
        val j = JSONObject(raw)
        if (!j.optBoolean("ok")) error("Telegram: ${j.optString("description", raw.take(150))}")
        return j
    }
    private fun form(vararg kv: Pair<String, String>) = FormBody.Builder().apply { kv.forEach { add(it.first, it.second) } }.build()

    override suspend fun publish(l: Listing): PubResult = withContext(Dispatchers.IO) {
        val text = l.postText(); val photos = l.ordered().take(10)
        require(text.length <= if (photos.isEmpty()) 4096 else 1024) { "Сократите текст объявления для Telegram" }
        val ids = mutableListOf<Long>()
        if (photos.isEmpty()) {
            ids += call("sendMessage", form("chat_id" to chat, "text" to text)).getJSONObject("result").getLong("message_id")
        } else if (photos.size == 1) {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chat).addFormDataPart("caption", text)
                .addFormDataPart("photo", "photo.jpg", Img.jpeg(photos[0], 1600).toRequestBody("image/jpeg".toMediaType())).build()
            ids += call("sendPhoto", body).getJSONObject("result").getLong("message_id")
        } else {
            val mb = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("chat_id", chat)
            val media = JSONArray()
            photos.forEachIndexed { i, p ->
                mb.addFormDataPart("p$i", "p$i.jpg", Img.jpeg(p, 1600).toRequestBody("image/jpeg".toMediaType()))
                media.put(JSONObject().put("type", "photo").put("media", "attach://p$i").apply { if (i == 0) put("caption", text) })
            }
            mb.addFormDataPart("media", media.toString())
            val arr = call("sendMediaGroup", mb.build()).getJSONArray("result")
            for (i in 0 until arr.length()) ids += arr.getJSONObject(i).getLong("message_id")
        }
        require(ids.isNotEmpty()) { "API не вернул ID публикации" }
        val url = if (chat.startsWith("@")) "https://t.me/${chat.drop(1)}/${ids[0]}" else ""
        PubResult("published", ids.joinToString(","), "ph=${if (photos.isEmpty()) 0 else 1};t=;chat=$chat", url, "Опубликовано в Telegram")
    }

    override suspend fun updatePrice(l: Listing, p: Publication): PubResult = withContext(Dispatchers.IO) {
        val ids = p.externalId.split(',').map { it.toLong() }; val m = metaMap(p.meta); val text = l.postText()
        require(m["chat"].isNullOrBlank() || m["chat"] == chat) { "Канал отличается от исходного" }
        val tid = m["t"].orEmpty()
        require(text.length <= if (m["ph"] == "1" && tid.isBlank()) 1024 else 4096) { "Сократите текст объявления для Telegram" }
        when {
            tid.isNotBlank() -> call("editMessageText", form("chat_id" to chat, "message_id" to tid, "text" to text.take(4096)))
            m["ph"] == "1" -> call("editMessageCaption", form("chat_id" to chat, "message_id" to ids[0].toString(), "caption" to text.take(1024)))
            else -> call("editMessageText", form("chat_id" to chat, "message_id" to ids[0].toString(), "text" to text.take(4096)))
        }
        PubResult("published", p.externalId, p.meta, p.url, "Цена обновлена")
    }

    override suspend fun unpublish(l: Listing, p: Publication): PubResult = withContext(Dispatchers.IO) {
        require(metaMap(p.meta)["chat"].isNullOrBlank() || metaMap(p.meta)["chat"] == chat) { "Канал отличается от исходного" }
        val ids = p.externalId.split(',') + listOfNotNull(metaMap(p.meta)["t"]?.takeIf { it.isNotBlank() })
        ids.forEach { id ->
            try { call("deleteMessage", form("chat_id" to chat, "message_id" to id)) }
            catch (e: IllegalStateException) { if (!e.message.orEmpty().contains("message to delete not found", ignoreCase = true)) throw e }
        }
        PubResult("removed", message = "Сообщения удалены из Telegram")
    }
}

/** Токен пользователя/сообщества с правами wall, photos. owner: id группы со знаком «-» или id пользователя. */
class VkPublisher(private val token: String, private val owner: String) : Publisher {
    private val group = owner.removePrefix("-").takeIf { owner.startsWith("-") }
    private fun api(method: String, params: Map<String, String>): JSONObject {
        val fb = FormBody.Builder().add("access_token", token).add("v", "5.199")
        params.forEach { (k, v) -> fb.add(k, v) }
        val raw = send(Request.Builder().url("https://api.vk.com/method/$method").post(fb.build()).build())
        val j = JSONObject(raw)
        if (j.has("error")) error("VK: ${j.getJSONObject("error").optString("error_msg")}")
        require(j.has("response")) { "VK: нет подтверждения API" }
        return j
    }
    private fun gp() = if (group != null) mapOf("group_id" to group) else emptyMap()

    private fun upload(path: String): String {
        val up = api("photos.getWallUploadServer", gp()).getJSONObject("response").getString("upload_url")
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("photo", "photo.jpg", Img.jpeg(path, 2000).toRequestBody("image/jpeg".toMediaType())).build()
        val u = JSONObject(send(Request.Builder().url(up).post(body).build()))
        val saved = api("photos.saveWallPhoto", gp() + mapOf("photo" to u.getString("photo"), "server" to u.get("server").toString(), "hash" to u.getString("hash")))
            .getJSONArray("response").getJSONObject(0)
        return "photo${saved.getLong("owner_id")}_${saved.getLong("id")}"
    }

    override suspend fun publish(l: Listing): PubResult = withContext(Dispatchers.IO) {
        val att = l.ordered().take(10).joinToString(",") { upload(it) }
        val params = mutableMapOf("owner_id" to owner, "message" to l.postText(), "attachments" to att)
        if (group != null) params["from_group"] = "1"
        val id = api("wall.post", params).getJSONObject("response").getLong("post_id")
        PubResult("published", id.toString(), att, "https://vk.com/wall${owner}_$id", "Опубликовано во ВКонтакте")
    }
    override suspend fun updatePrice(l: Listing, p: Publication): PubResult = withContext(Dispatchers.IO) {
        require(p.url.isBlank() || p.url.startsWith("https://vk.com/wall${owner}_")) { "Владелец стены отличается от исходного" }
        val result = api("wall.edit", mapOf("owner_id" to owner, "post_id" to p.externalId, "message" to l.postText(), "attachments" to p.meta)).get("response")
        require(result == 1 || (result is JSONObject && result.optLong("post_id") == p.externalId.toLongOrNull())) { "VK: изменение не подтверждено" }
        PubResult("published", p.externalId, p.meta, p.url, "Цена обновлена")
    }
    override suspend fun unpublish(l: Listing, p: Publication): PubResult = withContext(Dispatchers.IO) {
        require(p.url.isBlank() || p.url.startsWith("https://vk.com/wall${owner}_")) { "Владелец стены отличается от исходного" }
        require(api("wall.delete", mapOf("owner_id" to owner, "post_id" to p.externalId)).optInt("response") == 1) { "VK: удаление не подтверждено" }
        PubResult("removed", message = "Запись удалена")
    }
}

/**
 * VT 24 («Владис»): документации и доступа к API нет — коннектор НЕ реализован.
 * Сейчас: экспорт карточки (listings.json + тексты + фото) в ZIP. Для подключения реализуйте Publisher и добавьте в PublishVm.publisher().
 */
object Vt24 { const val CONNECTED = false }
