package ru.vladis.realtorai.agent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.vladis.realtorai.hunter.HunterDao
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.vladis.realtorai.data.Store
import ru.vladis.realtorai.hunter.HunterImport
import ru.vladis.realtorai.hunter.Offer
import java.util.concurrent.TimeUnit

internal object SearchServer {
    private val monitorLock = Mutex()
    private val gson = Gson()
    private val http = OkHttpClient.Builder().followRedirects(false)
        .callTimeout(120, TimeUnit.SECONDS).build()
    data class Result(val offers: List<Offer>, val sources: List<SearchSourceStatus>)

    fun connected(ctx: Context) = Store(ctx).searchServerUrl.isNotBlank()
    fun valid(value: String) = value.toHttpUrlOrNull()?.let {
        it.isHttps && it.username.isEmpty() && it.password.isEmpty() && it.query == null && it.fragment == null
    } == true

    private suspend fun request(ctx: Context, path: String, body: JsonObject? = null, delete: Boolean = false): JsonObject =
        withContext(Dispatchers.IO) {
            val store = Store(ctx)
            check(valid(store.searchServerUrl) && store.searchServerToken.isNotBlank()) { "Настройте HTTPS и токен сервера" }
            val request = Request.Builder().url(store.searchServerUrl.trimEnd('/') + path)
                .header("Authorization", "Bearer ${store.searchServerToken}")
            if (delete) request.delete()
            if (body != null) request.post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            http.newCall(request.build()).execute().use { response ->
                check(response.isSuccessful) { "Сервер поиска: HTTP ${response.code}" }
                val source = checkNotNull(response.body).source()
                check(!source.request(5_000_001)) { "Ответ сервера слишком большой" }
                JsonParser.parseString(source.readUtf8()).asJsonObject
            }
        }

    private fun payload(query: ListingQuery) = JsonObject().apply {
        addProperty("place", query.place)
        addProperty("deal", query.deal ?: "sale")
        query.market?.let { addProperty("market", it) }
        query.seller?.let { addProperty("seller", it) }
        query.budget.takeIf { it < Long.MAX_VALUE }?.let { addProperty("budget", it) }
        query.rooms?.let { addProperty("rooms", it) }
        addProperty("minArea", query.minArea)
    }

    private fun offers(rows: Iterable<com.google.gson.JsonElement>) = rows.mapNotNull { element ->
        val row = element.asJsonObject
        val label = row.get("source")?.asString ?: return@mapNotNull null
        HunterImport.fromJson("[" + row.toString() + "]", "search:$label").firstOrNull()
    }

    suspend fun search(ctx: Context, query: ListingQuery): Result {
        val response = request(ctx, "/v1/search", payload(query))
        val status = response.getAsJsonArray("sources").map { part ->
            val row = part.asJsonObject
            SearchSourceStatus(row["source"].asString, row["count"].asInt, row["status"].asString)
        }
        return Result(offers(response.getAsJsonArray("offers")), status)
    }

    suspend fun syncMonitor(ctx: Context, dao: HunterDao): HunterImport.Result = monitorLock.withLock {
        val prefs = ctx.getSharedPreferences("hunter", 0)
        val enabled = prefs.getBoolean("monitoring", false)
        val query = prefs.getString("serverQuery", null)?.let { gson.fromJson(it, ListingQuery::class.java) }
        val previous = prefs.getLong("serverWatch", 0L)
        if (!enabled || query == null) {
            if (previous > 0L) request(ctx, "/v1/watch/$previous", delete = true)
            prefs.edit().remove("serverWatch").remove("serverWatchCursor").apply()
            return@withLock HunterImport.ingest(dao, emptyList())
        }
        val data = request(ctx, "/v1/watch", payload(query))
        val watch = data["id"].asLong
        if (previous != watch) {
            if (previous > 0) request(ctx, "/v1/watch/$previous", delete = true)
            prefs.edit().putLong("serverWatch", watch).remove("serverWatchCursor").apply()
        }
        val cursor = prefs.getLong("serverWatchCursor", 0L)
        val events = request(ctx, "/v1/events?watch=$watch&after=$cursor").getAsJsonArray("events")
        val batch = events.map { it.asJsonObject }
        val result = HunterImport.ingest(dao, offers(batch.map { it["offer"] }).filter(query::matches))
        batch.lastOrNull()?.let { prefs.edit().putLong("serverWatchCursor", it["id"].asLong).apply() }
        result
    }
}
