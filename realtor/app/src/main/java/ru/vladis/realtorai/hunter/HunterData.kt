package ru.vladis.realtorai.hunter

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Объявление из ИМПОРТИРОВАННЫХ данных (файл/фид/текст/подключённый источник). Ничего не генерируется. */
@Entity(indices = [Index(value = ["source", "externalId"])])
data class Offer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val ownerName: String? = null, val ownerPhone: String? = null,
    val housingKind: String? = null, val externalId: String = "", val url: String = "",
    val title: String = "", val address: String = "", val district: String = "",
    val lat: Double? = null, val lon: Double? = null,
    val deal: String = "sale",         // sale | rent (для rent price = аренда в месяц)
    val market: String = "secondary",  // primary | secondary
    val price: Long = 0, val area: Double = 0.0, val rooms: Int = 0, val floor: Int = 0, val floorsTotal: Int = 0,
    val fingerprint: String = "", val favorite: Boolean = false, val active: Boolean = true,
    val region: String? = null, val locality: String? = null, val transit: String? = null,
    val seller: String? = null, val expiresAt: Long? = null,
    val metroWalkMinutes: Int? = null, val metroWalkByStation: String? = null,
    val photoUrls: String? = null, val sourceDescription: String? = null,
    val sourceUpdatedAt: Long? = null, val contactName: String? = null, val contactPhone: String? = null,
    val firstSeen: Long = System.currentTimeMillis(), val lastSeen: Long = System.currentTimeMillis())

@Entity data class PricePoint(@PrimaryKey(autoGenerate = true) val id: Long = 0, val offerId: Long, val price: Long, val at: Long = System.currentTimeMillis())

@Entity data class SavedFilter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String = "",
    val maxPrice: Long = 0, val minArea: Double = 0.0, val rooms: String = "", val district: String = "",
    val onlyBelowMarket: Boolean = false, val notify: Boolean = true)

/** Машиночитаемый фид (CSV/JSON по URL), к которому у пользователя есть законный доступ. */
@Entity data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val url: String,
    val format: String = "csv", val lastFetch: Long = 0, val lastStatus: String = "")

@Dao interface HunterDao {
    @Transaction
    suspend fun importBatch(rows: List<Offer>, now: Long): HunterImport.Result =
        HunterImport.applyBatch(this, rows, now)

    @Query("UPDATE Offer SET active=0 WHERE source=:source AND active=1 AND lastSeen<:before")
    suspend fun deactivateOlder(source: String, before: Long): Int

    @Query("SELECT * FROM Offer") fun offers(): Flow<List<Offer>>
    @Query("SELECT * FROM Offer WHERE id=:id") suspend fun offer(id: Long): Offer?
    @Query("SELECT * FROM Offer") suspend fun allOffers(): List<Offer>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveOffer(o: Offer): Long
    @Query("DELETE FROM Offer WHERE id=:id") suspend fun deleteOffer(id: Long)
    @Query("DELETE FROM PricePoint WHERE offerId=:id") suspend fun deletePoints(id: Long)
    @Query("SELECT * FROM PricePoint ORDER BY at") fun points(): Flow<List<PricePoint>>
    @Query("SELECT * FROM PricePoint WHERE offerId=:id ORDER BY at") fun pointsOf(id: Long): Flow<List<PricePoint>>
    @Insert suspend fun savePoint(p: PricePoint): Long
    @Query("SELECT * FROM SavedFilter") fun filters(): Flow<List<SavedFilter>>
    @Query("SELECT * FROM SavedFilter") suspend fun allFilters(): List<SavedFilter>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveFilter(f: SavedFilter): Long
    @Delete suspend fun deleteFilter(f: SavedFilter)
    @Query("SELECT * FROM Subscription") fun subs(): Flow<List<Subscription>>
    @Query("SELECT * FROM Subscription") suspend fun allSubs(): List<Subscription>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSub(s: Subscription): Long
    @Delete suspend fun deleteSub(s: Subscription)
}

object HunterDb {
    fun get(ctx: Context): HunterDao = ru.vladis.realtorai.data.WorkspaceDb.get(ctx).hunter()
}
