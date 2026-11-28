package ru.vladis.realtorai.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** API-ключ хранится в EncryptedSharedPreferences (Android Keystore). Остальные настройки — обычные prefs. */
class Store(ctx: Context) {
    private val secure = EncryptedSharedPreferences.create(
        ctx, "secure", MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    private val plain = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = "" // local-only build: cloud AI disabled
        set(v) { secure.edit().putString("groq_key", v).apply() }
    // Separate opt-in key: legacy import/publish paths remain local-only.
    var cloudKey: String
        get() = secure.getString("groq_cloud_key", "").orEmpty()
        set(v) { secure.edit().putString("groq_cloud_key", v).apply() }
    var searchServerUrl: String
        get() = plain.getString("search_server_url", "").orEmpty()
        set(value) { plain.edit().putString("search_server_url", value).apply() }
    var searchServerToken: String
        get() = secure.getString("search_server_token", "").orEmpty()
        set(value) { secure.edit().putString("search_server_token", value).apply() }
    var propertyIndexKey: String
        get() = secure.getString("property_index_key", "").orEmpty()
        set(value) { secure.edit().putString("property_index_key", value).apply() }
    var searchConversation: String
        get() = secure.getString("listing_search_context", "").orEmpty()
        set(value) { secure.edit().putString("listing_search_context", value).apply() }
    var transcript: String
        get() = secure.getString("assistant_transcript", "").orEmpty()
        set(value) { secure.edit().putString("assistant_transcript", value).apply() }
    var cloudEnabled: Boolean
        get() = plain.getBoolean("groq_cloud_enabled", false)
        set(v) { plain.edit().putBoolean("groq_cloud_enabled", v).apply() }
    var model: String
        get() = plain.getString("model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
        set(v) { plain.edit().putString("model", v).apply() }
    /** Комиссия агентства, % от цены объекта */
    var agencyPct: Double
        get() = plain.getFloat("agency", 3f).toDouble()
        set(v) { plain.edit().putFloat("agency", v.toFloat()).apply() }
    /** Доля агента от комиссии агентства, % */
    var sharePct: Double
        get() = plain.getFloat("share", 40f).toDouble()
        set(v) { plain.edit().putFloat("share", v.toFloat()).apply() }
}
