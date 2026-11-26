package ru.vladis.realtorai.owner

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class OwnerCase(
    @PrimaryKey val id: Long,
    val step: String,
    val startedAt: Long,
    val activatedAt: Long?,
    val reportsPerWeek: Int,
    val fast: Double,
    val normal: Double,
    val patient: Double,
    val location: String,
    val lastReportAt: Long?,
    val advertising: String,
    val packaging: String,
    val tour: String,
    val calls: Int?,
    val leads: Int?,
    val views: Int?,
    val ownerCalls: Int?,
    val impressions: Int?
)

@Entity(indices = [Index("propertyId")])
data class OwnerEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val at: Long,
    val kind: String,
    val text: String
)

@Dao
interface OwnerDao {
    @Query("SELECT * FROM OwnerCase WHERE id=:id") fun observe(id: Long): Flow<OwnerCase?>
    @Query("SELECT * FROM OwnerCase WHERE id=:id") suspend fun get(id: Long): OwnerCase?
    @Query("SELECT * FROM OwnerCase WHERE step != 'FINISHED'") suspend fun active(): List<OwnerCase>
    @Query("SELECT * FROM OwnerEvent WHERE propertyId=:id ORDER BY at DESC, id DESC") fun history(id: Long): Flow<List<OwnerEvent>>
    @Query("SELECT * FROM OwnerEvent WHERE propertyId=:id AND kind='draft' ORDER BY at DESC LIMIT 1")
    suspend fun latestDraft(id: Long): OwnerEvent?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(record: OwnerCase)
    @Insert suspend fun event(event: OwnerEvent)
    @Query("DELETE FROM OwnerEvent WHERE propertyId=:id") suspend fun clearEvents(id: Long)
    @Query("DELETE FROM OwnerCase WHERE id=:id") suspend fun delete(id: Long)
}
