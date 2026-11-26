package ru.vladis.realtorai.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceImportTest {
    @Test fun copiesLinkedRecordsOnceAcrossReopens() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = "migration-fixture.db"
        val legacy = "hunter.db"
        ctx.deleteDatabase(fixture)
        ctx.deleteDatabase(legacy)
        try {
            ctx.openOrCreateDatabase(legacy, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL("CREATE TABLE Offer (id INTEGER PRIMARY KEY)")
                old.execSQL("CREATE TABLE PricePoint (id INTEGER PRIMARY KEY, offerId INTEGER, price INTEGER, at INTEGER)")
                old.execSQL("CREATE TABLE SavedFilter (id INTEGER PRIMARY KEY)")
                old.execSQL("CREATE TABLE Subscription (id INTEGER PRIMARY KEY, name TEXT, url TEXT, format TEXT, lastFetch INTEGER, lastStatus TEXT)")
                old.execSQL("INSERT INTO Subscription VALUES (41,'Фид','https://example.org','csv',7,'ok')")
                old.execSQL("INSERT INTO PricePoint VALUES (6,12,7900000,111)")
            }
            repeat(2) {
                val db = Room.databaseBuilder(ctx, AppDb::class.java, fixture)
                    .addCallback(LegacyImport(ctx)).build()
                try {
                    assertEquals(41L, db.hunter().allSubs().single().id)
                    val sql = db.openHelper.readableDatabase
                    sql.query("SELECT offerId, price FROM PricePoint WHERE id=6").use { rows ->
                        check(rows.moveToFirst())
                        assertEquals(12L, rows.getLong(0))
                        assertEquals(7900000L, rows.getLong(1))
                    }
                } finally { db.close() }
            }
        } finally {
            ctx.deleteDatabase(fixture)
            ctx.deleteDatabase(legacy)
        }
    }
}
