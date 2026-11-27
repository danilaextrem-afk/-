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

    private val matchingMigration = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Client ADD COLUMN preferredMarket TEXT")
            db.execSQL("ALTER TABLE Client ADD COLUMN wishes TEXT")
        }
    }

    private val housingMigration = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Offer ADD COLUMN housingKind TEXT")
            db.execSQL("ALTER TABLE Property ADD COLUMN housingKind TEXT")
            db.execSQL("ALTER TABLE Client ADD COLUMN desiredKind TEXT")
        }
    }

    internal val ownerWorkflow = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS OwnerCase (id INTEGER NOT NULL PRIMARY KEY, step TEXT NOT NULL, startedAt INTEGER NOT NULL, activatedAt INTEGER, reportsPerWeek INTEGER NOT NULL, fast REAL NOT NULL, normal REAL NOT NULL, patient REAL NOT NULL, location TEXT NOT NULL, lastReportAt INTEGER, advertising TEXT NOT NULL, packaging TEXT NOT NULL, tour TEXT NOT NULL, calls INTEGER, leads INTEGER, views INTEGER, ownerCalls INTEGER, impressions INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS OwnerEvent (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, propertyId INTEGER NOT NULL, at INTEGER NOT NULL, kind TEXT NOT NULL, text TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_OwnerEvent_propertyId ON OwnerEvent (propertyId)")
        }
    }

    private val agentQueue = object : androidx.room.migration.Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS AgentJob (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, goal TEXT NOT NULL, plan TEXT NOT NULL, clientId INTEGER, state TEXT NOT NULL, result TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        }
    }

    private val moscowGeography = object : androidx.room.migration.Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            for (column in listOf("region", "locality", "transit", "seller")) {
                db.execSQL("ALTER TABLE Offer ADD COLUMN $column TEXT")
            }
            db.execSQL("ALTER TABLE Offer ADD COLUMN expiresAt INTEGER")
            db.execSQL("ALTER TABLE Property ADD COLUMN region TEXT")
        }
    }

    internal val listingDetails = object : androidx.room.migration.Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            for (column in listOf("photoUrls", "sourceDescription", "contactName", "contactPhone")) {
                db.execSQL("ALTER TABLE Offer ADD COLUMN $column TEXT")
            }
            db.execSQL("ALTER TABLE Offer ADD COLUMN sourceUpdatedAt INTEGER")
            db.execSQL("ALTER TABLE Property ADD COLUMN sourceOfferId INTEGER")
        }
    }

    internal val saleApproval = object : androidx.room.migration.Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Listing ADD COLUMN approvedAt INTEGER")
        }
    }

    private val transitEvidence = object : androidx.room.migration.Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Offer ADD COLUMN metroWalkMinutes INTEGER")
            db.execSQL("ALTER TABLE Offer ADD COLUMN metroWalkByStation TEXT")
            db.execSQL("ALTER TABLE Property ADD COLUMN ownerContactBlocked INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE Client ADD COLUMN doNotContact INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun get(context: Context): AppDb = instance ?: synchronized(this) {
        val app = context.applicationContext
        ru.vladis.realtorai.hunter.GeoScope.initialize(app)
        instance ?: Room.databaseBuilder(app, AppDb::class.java, "workspace.db")
            .addMigrations(criteriaMigration, ownerMigration, matchingMigration, housingMigration, ownerWorkflow, agentQueue, moscowGeography, listingDetails, saleApproval, transitEvidence)
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
