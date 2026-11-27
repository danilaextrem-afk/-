package ru.vladis.realtorai.office

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.data.Property

internal data class OfficeSession(val origin: String, val token: String)
internal data class Employee(val id: String, val name: String, val role: String) {
    val canAssign get() = role in setOf("admin", "manager")
}
internal enum class OfficeSection(val route: String, val label: String) {
    PROPERTIES("properties", "Объекты"), BUYERS("buyers", "Покупатели"), DEALS("deals", "Сделки")
}
internal data class OfficeRow(
    val id: String, val assignedTo: String, val revision: Int, val data: JSONObject,
    val propertyTitle: String, val buyerName: String
)
internal data class OfficePage(val rows: List<OfficeRow>, val next: String)

internal class OfficeSettings(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(context, "office_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    fun session(): OfficeSession? {
        val url = prefs.getString("origin", null) ?: return null
        val token = prefs.getString("token", null) ?: return null
        return OfficeSession(url, token)
    }

    fun save(session: OfficeSession) {
        prefs.edit().putString("origin", session.origin).putString("token", session.token).apply()
    }

    fun disconnect() { prefs.edit().remove("origin").remove("token").apply() }

    fun recordId(localId: Long): String {
        require(localId > 0)
        val prefix = prefs.getString("record_prefix", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("record_prefix", it).apply()
        }
        return "$prefix-$localId"
    }
}

private class OfficeConflict : IllegalStateException("Запись изменилась. Обновите список перед повтором.")

internal class OfficeClient(private val session: OfficeSession) {
    private val base = endpoint(session.origin)
    private val token = session.token.also {
        require(it.matches(Regex("[A-Za-z0-9_-]{43}"))) { "Введите токен, выданный офисом" }
    }

    private suspend fun request(
        path: List<String>, after: String? = null, method: String = "GET", data: JSONObject? = null,
        revision: Int? = null, create: Boolean = false
    ): JSONObject = withContext(Dispatchers.IO) {
        val endpoint = base.newBuilder().addPathSegment("v1").apply {
            path.forEach { addPathSegment(it) }
            if (!after.isNullOrBlank()) addQueryParameter("after", after)
        }.build()
        val request = Request.Builder().url(endpoint).header("Authorization", "Bearer $token")
            .header("Accept", "application/json").method(method, data?.toString()?.toRequestBody(jsonType)).apply {
                if (revision != null) header("If-Match", "\"$revision\"")
                if (create) header("If-None-Match", "*")
            }.build()
        http.newCall(request).execute().use { response ->
            if (response.code == 412) throw OfficeConflict()
            if (response.code == 409) throw IllegalStateException(
                when (runCatching { JSONObject(response.peekBody(2_048L).string()).optString("error") }.getOrNull()) {
                    "feed_not_configured" -> "Офис не подключил корпоративный фид."
                    "advertising_disabled" -> "Администратор приостановил размещение."
                    "placement_limit" -> "Все места в корпоративном фиде заняты."
                    "limit_below_active_count" -> "Лимит ниже числа действующих размещений."
                    "incomplete_secondary_property" -> "Для фида нужна вторичная квартира с адресом, ценой, площадью и комнатами."
                    "property_sold" -> "По объекту уже закрыта сделка."
                    "duplicate_deal" -> "По этой паре уже открыта сделка."
                    "invalid_stage" -> "Этот переход недоступен. Обновите список сделок."
                    else -> "Действие конфликтует с текущим состоянием общей базы."
                }
            )
            if (response.code == 404) throw NoSuchElementException("Запись в офисе не найдена")
            check(response.isSuccessful) { when (response.code) {
                401 -> "Сервер отклонил токен. Проверьте доступ."
                403 -> "У этой учётной записи нет прав для действия."
                503 -> "Общая база временно недоступна."
                else -> "Сервер офиса отклонил запрос (HTTP ${response.code})."
            } }
            val stream = checkNotNull(response.body).source()
            check(!stream.request(1_000_001)) { "Ответ офиса превышает ограничение" }
            JSONObject(stream.readUtf8())
        }
    }

    suspend fun advertisingPolicy() = request(listOf("advertising", "policy"))

    suspend fun saveAdvertisingPolicy(enabled: Boolean, limit: Int) = request(
        listOf("advertising", "policy"), method = "PUT",
        data = JSONObject().put("enabled", enabled).put("active_limit", limit)
    )

    suspend fun feedOffer(id: String) = request(listOf("advertising", "yandex", id))

    suspend fun approveFeedOffer(row: OfficeRow, offerRevision: Int, materials: JSONObject) = request(
        listOf("advertising", "yandex", row.id), method = "PUT", revision = offerRevision,
        data = materials.put("property_revision", row.revision)
    )

    suspend fun withdrawFeedOffer(id: String, offerRevision: Int) = request(
        listOf("advertising", "yandex", id), method = "DELETE", revision = offerRevision
    )

    suspend fun me(): Employee = employee(request(listOf("me")))

    suspend fun employees(): List<Employee> = request(listOf("employees")).objects().map(::employee)

    suspend fun records(section: OfficeSection, after: String? = null): OfficePage {
        val response = request(listOf(section.route), after)
        return OfficePage(response.objects().map(::row), response.optString("next"))
    }

    suspend fun share(section: OfficeSection, id: String, data: JSONObject, revision: Int?): OfficeRow =
        row(request(listOf(section.route, id), method = "PUT", data = data, revision = revision, create = revision == null))

    suspend fun assign(section: OfficeSection, row: OfficeRow, employee: Employee) =
        row(request(listOf(section.route, row.id), method = "PATCH",
            data = JSONObject().put("assigned_to", employee.id), revision = row.revision))

    private fun JSONObject.objects(): List<JSONObject> = getJSONArray("items").let { items ->
        (0 until items.length()).map(items::getJSONObject)
    }
    private fun employee(json: JSONObject) = Employee(json.getString("id"), json.getString("name"), json.getString("role"))
    private fun row(json: JSONObject) = OfficeRow(json.getString("id"), json.getString("assigned_to"),
        json.getInt("revision"), json.getJSONObject("data"),
        json.optString("property_title"), json.optString("buyer_name"))

    companion object {
        private val jsonType = "application/json".toMediaType()
        private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .callTimeout(15, TimeUnit.SECONDS).build()

        private fun endpoint(raw: String): HttpUrl {
            val url = raw.trim().toHttpUrlOrNull()
            require(url != null && url.isHttps && url.encodedPath == "/" && url.fragment == null &&
                url.query == null && url.username.isEmpty() && url.password.isEmpty()) {
                "Введите адрес HTTPS-сервера, например https://office.example.ru"
            }
            return url
        }
    }
}

internal fun Property.officeData() = JSONObject()
    .put("title", officeText(title, 160)).put("address", officeText(address, 240))
    .put("market", type).put("price", price).put("area", area).put("rooms", rooms)

internal fun Client.officeData() = JSONObject()
    .put("name", officeText(name, 120)).put("district", officeText(searchArea.orEmpty(), 160))
    .put("budget", budget).put("rooms", desiredRooms).put("min_area", minArea)

private fun officeText(value: String, limit: Int) = value.map { if (it.isISOControl()) ' ' else it }
    .joinToString("").trim().take(limit)
