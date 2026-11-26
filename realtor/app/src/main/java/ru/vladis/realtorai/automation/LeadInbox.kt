package ru.vladis.realtorai.automation

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.data.WorkspaceDb
import ru.vladis.realtorai.local.OfflineLogic
import ru.vladis.realtorai.publish.PublishSecrets
import java.util.concurrent.TimeUnit

object LeadInbox {
    private val http = OkHttpClient.Builder().followRedirects(false).callTimeout(25, TimeUnit.SECONDS).build()
    private fun config(ctx: Context) = PublishSecrets(ctx)
    fun connected(ctx: Context) = config(ctx).get("lead_url").isNotBlank()
    fun connect(ctx: Context, address: String, token: String) {
        if (address.isNotBlank()) {
            val url = address.toHttpUrl()
            require(url.isHttps && url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "Укажите HTTPS-адрес без параметров и паролей" }
        }
        config(ctx).apply { set("lead_url", address); set("lead_token", token) }
    }
    suspend fun refresh(ctx: Context): String {
        val address = config(ctx).get("lead_url").ifBlank { return "Подключите фид входящих заявок в разделе «Лиды»." }
        val token = config(ctx).get("lead_token")
        val rows = withContext(Dispatchers.IO) {
            val req = Request.Builder().url(address).apply { if (token.isNotBlank()) header("Authorization","Bearer $token") }.build()
            http.newCall(req).execute().use { res ->
                check(res.isSuccessful) { "Канал заявок: HTTP ${res.code}" }
                val source = checkNotNull(res.body).source()
                require(!source.request(2_000_001L)) { "Фид заявок превышает 2 МБ" }
                JsonParser.parseString(source.readUtf8()).asJsonArray
            }
        }
        val db = WorkspaceDb.get(ctx)
        return db.withTransaction {
            val dao = db.dao()
            val seen = dao.allClients().mapTo(mutableSetOf()) { OfflineLogic.phoneKey(it.phone) }
            var added = 0
            for (row in rows) {
                val item = row.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                fun value(k: String) = item.get(k)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty().trim()
                val consent = item.get("crm_allowed")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
                if (consent?.isBoolean != true || !consent.asBoolean) continue
                val name = value("name")
                val phone = OfflineLogic.phoneKey(value("phone"))
                val needs = value("message").take(500)
                val role = when (value("role")) {
                    "owner", "buyer" -> value("role")
                    else -> when {
                        Regex("прода[юём]|собственник", RegexOption.IGNORE_CASE).containsMatchIn(needs) -> "owner"
                        Regex("купить|покупаю|ищу", RegexOption.IGNORE_CASE).containsMatchIn(needs) -> "buyer"
                        else -> continue
                    }
                }
                if (name.isBlank() || phone.length !in 7..15 || !seen.add(phone)) continue
                dao.saveClient(Client(name = name.take(100), phone = phone, role = role,
                    budget = value("budget").toLongOrNull()?.takeIf { it > 0 } ?: 0,
                    searchArea = value("district").take(100).ifBlank { null },
                    desiredRooms = value("rooms").toIntOrNull()?.takeIf { it in 1..20 } ?: 0,
                    minArea = value("min_area").toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: 0.0,
                    wishes = value("wishes").take(300).ifBlank { null },
                    notes = "Источник: входящая заявка\n$needs"))
                added++
            }
            "Проверено заявок: ${rows.size()} · добавлено: $added"
        }
    }
}
