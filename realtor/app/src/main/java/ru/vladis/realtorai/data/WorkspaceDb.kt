package ru.vladis.realtorai.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object WorkspaceDb {
    @Volatile private var instance: AppDb? = null

    private val criteriaMigration = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Client ADD COLUMN desiredRooms INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE Client ADD COLUMN minArea REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE Client ADD COLUMN searchArea TEXT")
        }
    }

    private val ownerMigration = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Offer ADD COLUMN ownerName TEXT")
            db.execSQL("ALTER TABLE Offer ADD COLUMN ownerPhone TEXT")
        }
    }

    fun get(context: Context): AppDb = instance ?: synchronized(this) {
        val app = context.applicationContext
        instance ?: Room.databaseBuilder(app, AppDb::class.java, "workspace.db")
            .addMigrations(criteriaMigration, ownerMigration)
            .addCallback(LegacyImport(app)).build().also { instance = it }
    }
}

// Room invokes onCreate inside SQLiteOpenHelper's creation transaction.
// A failed copy rolls back all tables and retries on the next open.
internal class LegacyImport(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val sources = mapOf(
            "realtor.db" to listOf("Property", "Client", "Interaction", "TaskItem"),
            "hunter.db" to listOf("Offer", "PricePoint", "SavedFilter", "Subscription"),
            "publish.db" to listOf("Listing", "Publication")
        )
        for ((name, tables) in sources) {
            val path = context.getDatabasePath(name)
            if (!path.exists()) continue
            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { legacy ->
                for (table in tables) copyTable(legacy, db, table)
            }
        }
    }

    private fun copyTable(source: SQLiteDatabase, target: SupportSQLiteDatabase, table: String) {
        source.rawQuery("SELECT * FROM `$table`", null).use { rows ->
            val columns = rows.columnNames.joinToString { "`" + it.replace("`", "``") + "`" }
            val parameters = rows.columnNames.joinToString { "?" }
            target.compileStatement("INSERT INTO `$table` ($columns) VALUES ($parameters)").use { insert ->
                while (rows.moveToNext()) {
                    for (i in rows.columnNames.indices) when (rows.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> insert.bindNull(i + 1)
                        Cursor.FIELD_TYPE_INTEGER -> insert.bindLong(i + 1, rows.getLong(i))
                        Cursor.FIELD_TYPE_FLOAT -> insert.bindDouble(i + 1, rows.getDouble(i))
                        Cursor.FIELD_TYPE_BLOB -> insert.bindBlob(i + 1, rows.getBlob(i))
                        else -> insert.bindString(i + 1, rows.getString(i))
                    }
                    insert.executeInsert()
                }
            }
        }
    }
}
