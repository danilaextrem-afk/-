package ru.vladis.realtorai.agent

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ru.vladis.realtorai.hunter.GeoScope

internal data class SearchConversation(
    val current: ListingQuery? = null,
    val preferences: ListingQuery = ListingQuery(),
    val seen: List<String> = emptyList(),
    val recent: List<ListingQuery>? = null,
    val lastLowestPrice: Long? = null
)

internal data class SearchTurn(val kind: String, val query: ListingQuery)
internal data class ListingReply(val text: String, val ids: List<Long> = emptyList(), val keys: List<String> = emptyList(), val lowestPrice: Long? = null)

internal object SearchLanguage {
    private val gson = Gson()
    private val protocol = """
        Ты маршрутизатор запросов в рабочем приложении риелтора. Отвечай только JSON.
        kind: search (новый поиск), refine (продолжение или уточнение), remember (явная просьба запомнить предпочтения),
        либо chat (совет, обсуждение, подготовка документа и прочее). Даже короткая просьба показать недвижимость означает поиск.
        «Ещё», «следующие», «а в Химках?», «до 15 миллионов», «теперь только двушки» после выдачи означают refine.
        Вопрос о недвижимости сам по себе не всегда означает поиск.
        Для search/refine/remember верни {"kind":"...","criteria":{...}}. Для chat верни {"kind":"chat"}.
        Если пользователь просит «подешевле» относительно выдачи, передай cheaper: true.
        Поля criteria: area (район или город), budget (верхняя цена целым числом в рублях), rooms (целое, 0 для студии),
        minArea (м²), market (primary или secondary), seller (owner/developer/agent), address, station.
        Указывай только ограничения из нового сообщения, не додумывай. Чтобы снять ограничение передай null.
        Москва и Московская область разрешены по умолчанию. Без бюджета, района, комнат и площади поиск допустим.
        «От собственников» означает seller=owner. Для обычной просьбы без этого условия поле seller отсутствует.
    """.trimIndent()

    fun prompt(text: String, state: SearchConversation): Pair<String, String> {
        val context = gson.toJson(state.current)
        return protocol to "Предыдущие условия поиска: $context\nНовое сообщение: $text"
    }

    fun resolve(raw: String, state: SearchConversation): SearchTurn? {
        val value = runCatching {
            JsonParser.parseString(raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
                .takeIf { it.isJsonObject }?.asJsonObject
        }.getOrNull() ?: return null
        val kind = value.get("kind")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
        if (kind == "chat") return SearchTurn(kind, ListingQuery())
        if (kind !in setOf("search", "refine", "remember")) return null
        val values = value.get("criteria")?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
        val base = if (kind == "refine") state.current ?: state.preferences else state.preferences
        val result = patch(base, values)
        val cheaper = value.get("cheaper")?.takeIf { it.isJsonPrimitive }?.asString == "true"
        return SearchTurn(kind, if (kind == "refine" && cheaper) cheaper(result, state.lastLowestPrice) else result)
    }

    private fun cheaper(query: ListingQuery, previous: Long?): ListingQuery = query.copy(
        budget = previous?.takeIf { it > 1 }?.let { minOf(query.budget, it - 1) } ?: query.budget,
        cheapestFirst = true
    )

    private fun patch(base: ListingQuery, value: JsonObject): ListingQuery {
        fun field(key: String) = value.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()
        fun text(key: String, previous: String) = if (value.has(key)) field(key).orEmpty().take(160) else previous
        fun option(key: String, previous: String?, allowed: Set<String>) =
            if (value.has(key)) field(key)?.takeIf { it in allowed } else previous
        return base.copy(
            place = text("area", base.place), address = text("address", base.address), station = text("station", base.station),
            budget = if (value.has("budget")) field("budget")?.toLongOrNull()?.takeIf { it in 1..1_000_000_000_000L } ?: Long.MAX_VALUE else base.budget,
            rooms = if (value.has("rooms")) field("rooms")?.toIntOrNull()?.takeIf { it in 0..20 } else base.rooms,
            minArea = if (value.has("minArea")) field("minArea")?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100_000.0 } ?: 0.0 else base.minArea,
            market = option("market", base.market, setOf("primary", "secondary")),
            seller = option("seller", base.seller, setOf("owner", "developer", "agent"))
        )
    }

    fun offline(text: String, state: SearchConversation): SearchTurn? {
        val clean = GeoScope.normalize(text)
        val prior = state.current
        val inferred = ListingQuery.read(text)
        val more = clean in setOf("еще", "еще варианты", "давай еще", "продолжай", "следующие", "еще есть", "что еще есть")
        val wantsCheaper = "подешевле" in clean || clean == "дешевле"
        val qualifier = listOf("а ", "теперь ", "только ", "до ", "не дороже ", "не больше ", "без ").any(clean::startsWith)
        val continuation = prior != null && (more || wantsCheaper || qualifier && inferred != ListingQuery())
        if (!continuation && !ListingSearch.accepts(text)) return null
        val base = if (continuation) requireNotNull(prior) else state.preferences
        val query = base.copy(
            place = inferred.place.ifBlank { base.place }, address = inferred.address.ifBlank { base.address },
            station = inferred.station.ifBlank { base.station }, budget = inferred.budget.takeUnless { it == Long.MAX_VALUE } ?: base.budget,
            rooms = inferred.rooms ?: base.rooms, minArea = inferred.minArea.takeIf { it > 0 } ?: base.minArea,
            market = inferred.market ?: base.market, seller = inferred.seller ?: base.seller
        )
        return SearchTurn(if (continuation) "refine" else "search",
            if (wantsCheaper && continuation) cheaper(query, state.lastLowestPrice) else query)
    }
}
