package ru.vladis.realtorai.data

import android.content.Context
import android.database.Cursor
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.*
import java.io.*
import java.util.zip.*

object WorkspaceBackup {
    private val tables = listOf("Client", "Property", "Interaction", "TaskItem", "Offer", "PricePoint",
        "SavedFilter", "Subscription", "Listing", "Publication", "RunLog", "OwnerCase", "OwnerEvent", "AgentJob")
    private val legacy = mapOf("Client" to "clients", "Property" to "properties",
        "Interaction" to "interactions", "TaskItem" to "tasks")
    private const val limit = 48 * 1024 * 1024

    suspend fun write(ctx: Context, db: AppDb, output: OutputStream) = db.withTransaction {
        ZipOutputStream(output.buffered()).use { zip ->
            val root = JsonObject()
            val media = linkedMapOf<String, String>()
            for (table in tables) {
                val records = JsonArray()
                db.openHelper.readableDatabase.query("SELECT * FROM `$table`").use { cursor ->
                    while (cursor.moveToNext()) {
                        val row = JsonObject()
                        cursor.columnNames.forEachIndexed { i, name ->
                            when (cursor.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> row.add(name, JsonNull.INSTANCE)
                                Cursor.FIELD_TYPE_INTEGER -> row.addProperty(name, cursor.getLong(i))
                                Cursor.FIELD_TYPE_FLOAT -> row.addProperty(name, cursor.getDouble(i))
                                else -> row.addProperty(name, cursor.getString(i))
                            }
                        }
                        if (table == "Listing") photoPaths(row) { path ->
                            media.getOrPut(path) { "photo-${media.size}.jpg" }
                        }
                        records.add(row)
                    }
                }
                root.add(table, records)
            }
            zip.putNextEntry(ZipEntry("workspace.json"))
            val bytes = GsonBuilder().serializeNulls().create().toJson(root).toByteArray()
            require(bytes.size <= limit) { "База превышает лимит архива" }
            zip.write(bytes)
            val parent = File(ctx.filesDir, "publish").canonicalPath + File.separator
            for ((path, name) in media) {
                val file = File(path).canonicalFile
                require(file.path.startsWith(parent) && file.isFile) { "Недоступна фотография" }
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zip) }
            }
        }
    }

    private fun photoPaths(row: JsonObject, transform: (String) -> String) {
        for (field in listOf("photos", "cover")) {
            val value = row.get(field).asString.lines().filter { it.isNotBlank() }
            row.addProperty(field, value.joinToString("\n", transform = transform))
        }
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n == -1) break
            require(output.size() + n <= limit) { "Слишком большой файл данных" }
            output.write(buffer, 0, n)
        }
        return output.toByteArray()
    }

    suspend fun read(ctx: Context, db: AppDb, input: InputStream): List<TaskItem> {
        val stream = input.buffered()
        stream.mark(4)
        val archive = stream.read() == 80 && stream.read() == 75
        stream.reset()
        val staged = mutableListOf<File>()
        try {
            val root = if (!archive) JsonParser.parseString(readLimited(stream).decodeToString()).asJsonObject
            else ZipInputStream(stream).use { zip ->
                check(zip.nextEntry?.name == "workspace.json") { "Неверный архив" }
                val records = JsonParser.parseString(readLimited(zip).decodeToString()).asJsonObject
                val names = mutableMapOf<String, File>()
                for (item in records.getAsJsonArray("Listing")) {
                    val row = item.asJsonObject
                    val id = row.get("id").asLong
                    require(id > 0)
                    photoPaths(row) { name ->
                        require(Regex("photo-[0-9]+\\.jpg").matches(name))
                        names.getOrPut(name) {
                            val dir = File(ctx.filesDir, "publish/$id").apply { mkdirs() }
                            File.createTempFile("restore-", ".jpg", dir).also { staged += it }
                        }.absolutePath
                    }
                }
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val file = requireNotNull(names.remove(entry.name)) { "Неизвестный файл архива" }
                    file.outputStream().use { zip.copyTo(it) }
                }
                require(names.isEmpty()) { "Архив содержит не все фотографии" }
                records
            }
            val selection = if (archive) {
                val expected = when {
                    "AgentJob" in root.keySet() -> tables
                    "OwnerCase" in root.keySet() || "OwnerEvent" in root.keySet() -> tables.dropLast(1)
                    else -> tables.dropLast(3)
                }
                expected.associateWith { it }
            } else legacy
            require(root.keySet() == selection.values.toSet()) { "Неполная или неизвестная схема копии" }
            return db.withTransaction {
                val sql = db.openHelper.writableDatabase
                val oldTasks = db.dao().allTasks()
                if ("AgentJob" !in selection) sql.execSQL("DELETE FROM AgentJob")
                if ("OwnerCase" !in selection) {
                    sql.execSQL("DELETE FROM OwnerEvent")
                    sql.execSQL("DELETE FROM OwnerCase")
                }
                for ((table, key) in selection) stage(sql, table, root.getAsJsonArray(key), !archive)
                for (table in selection.keys) {
                    sql.execSQL("DELETE FROM `$table`")
                    sql.execSQL("INSERT INTO `$table` SELECT * FROM temp.backup_$table")
                    sql.execSQL("DROP TABLE temp.backup_$table")
                }
                sql.execSQL("UPDATE AgentJob SET state='cancelled' WHERE state IN ('queued','running')")
                oldTasks
            }
        } catch (e: Throwable) {
            staged.forEach { it.delete() }
            throw e
        }
    }

    private data class Column(val name: String, val type: String, val required: Boolean, val default: String?)

    private fun stage(sql: SupportSQLiteDatabase, table: String, rows: JsonArray, legacy: Boolean) {
        val columns = mutableListOf<Column>()
        sql.query("PRAGMA table_info(`$table`)").use { c ->
            while (c.moveToNext()) columns += Column(c.getString(1), c.getString(2), c.getInt(3) != 0, c.getString(4))
        }
        sql.execSQL("CREATE TEMP TABLE backup_$table AS SELECT * FROM `$table` WHERE 0")
        val ids = mutableSetOf<Long>()
        val names = columns.joinToString { "`${it.name}`" }
        val marks = columns.joinToString { "?" }
        sql.compileStatement("INSERT INTO temp.backup_$table ($names) VALUES ($marks)").use { stmt ->
            for (item in rows) {
                val row = item.asJsonObject
                require(row.get("id").asLong > 0 && ids.add(row.get("id").asLong)) { "Некорректный или повторный ID в $table" }
                for ((index, col) in columns.withIndex()) {
                    val pos = index + 1
                    val raw = row.get(col.name)
                    val v = when {
                        raw != null -> raw.takeUnless { it.isJsonNull }?.asJsonPrimitive
                        legacy && col.default == "0" -> JsonPrimitive(0)
                        !col.required && (legacy || (table == "Listing" && col.name == "approvedAt") || (table == "Property" && col.name == "region") ||
                            (table == "Offer" && col.name in setOf("region","locality","transit","seller","expiresAt"))) -> null
                        else -> error("Пропущено поле $table.${col.name}")
                    }
                    when {
                        v == null -> { require(!col.required); stmt.bindNull(pos) }
                        col.type == "INTEGER" -> stmt.bindLong(pos, if (legacy && v.isBoolean) if (v.asBoolean) 1L else 0L else v.asString.toLong())
                        col.type == "REAL" -> stmt.bindDouble(pos, v.asDouble.also { require(it.isFinite()) })
                        else -> { require(v.isString); stmt.bindString(pos, v.asString) }
                    }
                }
                stmt.executeInsert()
            }
        }
    }
}
