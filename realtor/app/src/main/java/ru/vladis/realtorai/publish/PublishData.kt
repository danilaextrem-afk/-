package ru.vladis.realtorai.publish

import android.content.Context
import androidx.room.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow

/** Единая карточка объекта для публикации. */
@Entity data class Listing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long? = null,
    val address: String = "", val price: Long = 0, val area: Double = 0.0,
    val rooms: Int = 0, val floor: Int = 0, val floorsTotal: Int = 0, val kitchenArea: Double = 0.0,
    val market: String = "secondary", // primary | secondary
    val renovation: String = "",      // со слов собственника/агента
    val extra: String = "",           // любые другие ПОДТВЕРЖДЁННЫЕ факты (единственный источник кроме полей и фото)
    val phone: String = "",
    val title: String = "", val description: String = "",
    val photos: String = "", val cover: String = "", val features: String = "", // пути файлов через \n
    val status: String = "draft",     // draft | sold
    val updatedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null)

fun Listing.photoList(): List<String> = photos.lines().filter { it.isNotBlank() }
/** Фото, обложка первой */
fun Listing.ordered(): List<String> = photoList().let { p -> if (cover in p) listOf(cover) + (p - cover) else p }

/** Статусы: none | published (подтверждено ответом API) | exported (файл сформирован, НЕ подтверждено) |
 *  failed | unknown (результат отправки не подтверждён) | removed | removal_pending | outdated */
@Entity(indices = [Index(value = ["listingId", "platform"], unique = true)])
data class Publication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listingId: Long, val platform: String, val status: String = "none",
    val externalId: String = "", val meta: String = "", val url: String = "", val message: String = "",
    val price: Long = 0, val updatedAt: Long = System.currentTimeMillis())

@Dao interface PublishDao {
    @Query("SELECT * FROM Listing ORDER BY updatedAt DESC") fun listings(): Flow<List<Listing>>
    @Query("SELECT * FROM Listing WHERE propertyId=:id ORDER BY id LIMIT 1") suspend fun listingForProperty(id: Long): Listing?
    @Query("SELECT * FROM Listing WHERE id=:id") suspend fun listing(id: Long): Listing?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveListing(l: Listing): Long
    @Delete suspend fun deleteListing(l: Listing)
    @Query("DELETE FROM Publication WHERE listingId=:id") suspend fun deletePubs(id: Long)
    @Query("SELECT * FROM Publication") fun publications(): Flow<List<Publication>>
    @Query("SELECT * FROM Publication WHERE listingId=:id") suspend fun pubsOf(id: Long): List<Publication>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePub(p: Publication): Long
}

/** Токены площадок — только в EncryptedSharedPreferences. */
class PublishSecrets(ctx: Context) {
    private val sp = EncryptedSharedPreferences.create(ctx, "publish_secure",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    fun get(k: String, def: String = "") = sp.getString(k, def) ?: def
    fun set(k: String, v: String) { sp.edit().putString(k, v.trim()).apply() }
}

internal fun PublishSecrets.publisher(platform: Platform): Publisher? = when (platform) {
    Platform.TELEGRAM -> get("tg_token").takeIf(String::isNotBlank)?.let { token ->
        get("tg_chat").takeIf(String::isNotBlank)?.let { TelegramPublisher(token, it) }
    }
    Platform.VK -> get("vk_token").takeIf(String::isNotBlank)?.let { token ->
        get("vk_owner").takeIf(String::isNotBlank)?.let { VkPublisher(token, it) }
    }
    else -> null
}
