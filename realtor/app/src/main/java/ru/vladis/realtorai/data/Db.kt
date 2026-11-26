package ru.vladis.realtorai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val address: String = "",
    val type: String = "secondary", // primary | secondary
    val price: Long = 0, val area: Double = 0.0, val rooms: Int = 0, val floor: Int = 0,
    val url: String = "", val description: String = "",
    val ownerId: Long? = null, val favorite: Boolean = false,
    val status: String = "active", // active | deal | archived
    val createdAt: Long = System.currentTimeMillis())

fun Property.pricePerSqm(): Long = if (area > 0) (price / area).toLong() else 0L

@Entity data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val phone: String = "",
    val role: String = "buyer", // buyer | owner
    val budget: Long = 0, val notes: String = "",
    val stage: String = "new", // new|contact|showing|deposit|deal|lost
    val createdAt: Long = System.currentTimeMillis())

@Entity data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long, val text: String, val at: Long = System.currentTimeMillis())

@Entity data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val kind: String = "task", // task | meeting
    val dueAt: Long, val done: Boolean = false)

@Dao interface AppDao {
    @Query("SELECT * FROM Property ORDER BY createdAt DESC") fun properties(): Flow<List<Property>>
    @Query("SELECT * FROM Client ORDER BY createdAt DESC") fun clients(): Flow<List<Client>>
    @Query("SELECT * FROM TaskItem ORDER BY dueAt") fun tasks(): Flow<List<TaskItem>>
    @Query("SELECT * FROM Interaction WHERE clientId=:id ORDER BY at DESC") fun interactions(id: Long): Flow<List<Interaction>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProperty(p: Property): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveClient(c: Client): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveInteraction(i: Interaction): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTask(t: TaskItem): Long
    @Delete suspend fun deleteProperty(p: Property)
    @Delete suspend fun deleteClient(c: Client)
    @Delete suspend fun deleteTask(t: TaskItem)
    @Query("DELETE FROM Interaction WHERE clientId=:id") suspend fun deleteInteractions(id: Long)
    @Query("SELECT * FROM Property") suspend fun allProperties(): List<Property>
    @Query("SELECT * FROM Client") suspend fun allClients(): List<Client>
    @Query("SELECT * FROM Interaction") suspend fun allInteractions(): List<Interaction>
    @Query("SELECT * FROM TaskItem") suspend fun allTasks(): List<TaskItem>
}

@Database(entities = [Property::class, Client::class, Interaction::class, TaskItem::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() { abstract fun dao(): AppDao }
