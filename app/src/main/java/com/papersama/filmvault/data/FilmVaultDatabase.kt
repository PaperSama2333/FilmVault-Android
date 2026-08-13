package com.papersama.filmvault.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class FilmVaultDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE rolls (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                brand TEXT NOT NULL,
                brand_name TEXT NOT NULL,
                type TEXT NOT NULL,
                iso TEXT NOT NULL,
                frames INTEGER NOT NULL,
                shot INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL,
                archived INTEGER NOT NULL DEFAULT 0,
                archived_date TEXT NOT NULL DEFAULT '',
                custom_tags TEXT NOT NULL DEFAULT '[]',
                lab_status TEXT NOT NULL DEFAULT 'none',
                lab_store TEXT NOT NULL DEFAULT '',
                lab_date TEXT NOT NULL DEFAULT '',
                lab_scanned INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE shots (
                id TEXT PRIMARY KEY NOT NULL,
                roll_id TEXT NOT NULL,
                date TEXT NOT NULL,
                count INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                samples TEXT NOT NULL DEFAULT '[]',
                FOREIGN KEY (roll_id) REFERENCES rolls(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_shots_roll_id ON shots(roll_id)")
        db.execSQL(
            """
            CREATE TABLE tags (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                UNIQUE(name, category)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE roll_tags (
                roll_id TEXT NOT NULL,
                tag_id TEXT NOT NULL,
                PRIMARY KEY (roll_id, tag_id),
                FOREIGN KEY (roll_id) REFERENCES rolls(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_roll_tags_tag ON roll_tags(tag_id)")
        db.execSQL("CREATE TABLE profile (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    fun loadState(): AppUiState {
        val db = readableDatabase
        val tags = loadTags(db)
        val tagsById = tags.associateBy(TagItem::id)
        val tagCounts = mutableMapOf<String, Int>()
        db.rawQuery("SELECT tag_id, COUNT(*) AS qty FROM roll_tags GROUP BY tag_id", null).use {
            while (it.moveToNext()) tagCounts[it.string("tag_id")] = it.int("qty")
        }

        val rolls = mutableListOf<Roll>()
        db.rawQuery("SELECT * FROM rolls ORDER BY rowid DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.string("id")
                val tagIds = mutableListOf<String>()
                db.rawQuery("SELECT tag_id FROM roll_tags WHERE roll_id = ?", arrayOf(id)).use {
                    while (it.moveToNext()) tagIds += it.string("tag_id")
                }
                val customTags = jsonStrings(cursor.string("custom_tags"))
                val visibleTags = tagIds.mapNotNull(tagsById::get)
                    .filter { it.category != TagCategory.TYPE }
                    .map(TagItem::name) + customTags
                val lab = LabRecord(
                    status = labStatusOf(cursor.string("lab_status")),
                    store = cursor.string("lab_store"),
                    date = cursor.string("lab_date"),
                    scanned = cursor.int("lab_scanned") == 1,
                )
                rolls += Roll(
                    id = id,
                    name = cursor.string("name"),
                    brand = cursor.string("brand"),
                    brandName = cursor.string("brand_name"),
                    type = cursor.string("type"),
                    iso = cursor.string("iso"),
                    frames = cursor.int("frames"),
                    shot = cursor.int("shot"),
                    status = rollStatusOf(cursor.string("status")),
                    archived = cursor.int("archived") == 1,
                    archivedDate = cursor.string("archived_date"),
                    tags = visibleTags.distinct(),
                    tagIds = tagIds,
                    customTags = customTags,
                    shots = loadShots(db, id),
                    lab = lab,
                )
            }
        }
        return AppUiState(
            rolls = rolls,
            tags = tags,
            tagCounts = tagCounts,
            profile = UserProfile(
                name = profileValue(db, "name", "胶片爱好者"),
                avatar = profileValue(db, "avatar", ""),
            ),
            loading = false,
        )
    }

    @Synchronized
    fun createRoll(input: NewRollInput): String {
        val db = writableDatabase
        val id = uid("roll-")
        db.beginTransaction()
        try {
            db.insertOrThrow("rolls", null, ContentValues().apply {
                put("id", id)
                put("name", input.name)
                put("brand", input.brandName)
                put("brand_name", input.brandName)
                put("type", input.type)
                put("iso", input.iso)
                put("frames", input.frames.coerceAtLeast(1))
                put("shot", 0)
                put("status", RollStatus.UNSHOT.name)
                put("custom_tags", stringsJson(input.customTags))
            })
            val automatic = loadTags(db).filter {
                (it.category == TagCategory.BRAND && it.name == input.brandName) ||
                    (it.category == TagCategory.TYPE && it.name == input.type)
            }.map(TagItem::id)
            (automatic + input.projectTagIds).distinct().forEach { addRollTag(db, id, it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return id
    }

    @Synchronized
    fun saveShot(rollId: String, shotId: String?, input: ShotInput) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("roll_id", rollId)
            put("date", input.date)
            put("count", input.count.coerceAtLeast(0))
            put("note", input.note)
            put("samples", stringsJson(input.samples))
        }
        if (shotId == null) {
            values.put("id", uid("shot-"))
            db.insertOrThrow("shots", null, values)
        } else {
            db.update("shots", values, "id = ? AND roll_id = ?", arrayOf(shotId, rollId))
        }
        recomputeRoll(db, rollId)
    }

    @Synchronized
    fun deleteShot(rollId: String, shotId: String) {
        val db = writableDatabase
        db.delete("shots", "id = ? AND roll_id = ?", arrayOf(shotId, rollId))
        recomputeRoll(db, rollId)
    }

    @Synchronized
    fun updateLab(rollId: String, lab: LabRecord) {
        val db = writableDatabase
        db.update("rolls", ContentValues().apply {
            put("lab_status", lab.status.value)
            put("lab_store", lab.store)
            put("lab_date", lab.date)
            put("lab_scanned", if (lab.scanned) 1 else 0)
        }, "id = ?", arrayOf(rollId))
        recomputeRoll(db, rollId)
    }

    @Synchronized
    fun markDone(id: String, date: String) {
        writableDatabase.update("rolls", ContentValues().apply {
            put("archived", 1)
            put("archived_date", date)
            put("status", RollStatus.DONE.name)
        }, "id = ?", arrayOf(id))
    }

    @Synchronized
    fun unarchive(id: String) {
        writableDatabase.update("rolls", ContentValues().apply {
            put("archived", 0)
            put("archived_date", "")
        }, "id = ?", arrayOf(id))
        recomputeRoll(writableDatabase, id)
    }

    @Synchronized
    fun deleteRoll(id: String) {
        writableDatabase.delete("rolls", "id = ?", arrayOf(id))
    }

    @Synchronized
    fun updateRollTags(rollId: String, projectTagIds: List<String>, customTags: List<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                """
                DELETE FROM roll_tags
                WHERE roll_id = ? AND tag_id IN (SELECT id FROM tags WHERE category = 'project')
                """.trimIndent(),
                arrayOf(rollId),
            )
            projectTagIds.distinct().forEach { addRollTag(db, rollId, it) }
            db.update("rolls", ContentValues().apply {
                put("custom_tags", stringsJson(customTags))
            }, "id = ?", arrayOf(rollId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun createTag(name: String, category: TagCategory) {
        writableDatabase.insertWithOnConflict("tags", null, ContentValues().apply {
            put("id", uid("tag-"))
            put("name", name)
            put("category", category.value)
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }

    @Synchronized
    fun renameTag(id: String, name: String) {
        writableDatabase.update("tags", ContentValues().apply { put("name", name) }, "id = ?", arrayOf(id))
    }

    @Synchronized
    fun deleteTag(id: String) {
        writableDatabase.delete("tags", "id = ?", arrayOf(id))
    }

    @Synchronized
    fun updateProfile(key: String, value: String) {
        writableDatabase.insertWithOnConflict("profile", null, ContentValues().apply {
            put("key", key)
            put("value", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun exportBackup(): String {
        val db = readableDatabase
        val root = JSONObject()
            .put("format", "FilmVault Android Backup")
            .put("schemaVersion", 1)
            .put("exportedAt", System.currentTimeMillis())
        val tables = JSONObject()
        BACKUP_TABLES.forEach { table ->
            tables.put(table, tableToJson(db, table, excludeAvatar = table == "profile"))
        }
        return root.put("tables", tables).toString(2)
    }

    @Synchronized
    fun importBackup(raw: String) {
        val root = JSONObject(raw)
        require(root.optString("format") == "FilmVault Android Backup") { "备份格式不正确" }
        val tables = root.getJSONObject("tables")
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("PRAGMA defer_foreign_keys = ON")
            listOf("roll_tags", "shots", "rolls", "tags", "profile").forEach {
                db.delete(it, null, null)
            }
            listOf("rolls", "tags", "shots", "roll_tags", "profile").forEach { table ->
                val rows = tables.optJSONArray(table) ?: JSONArray()
                for (index in 0 until rows.length()) {
                    val objectRow = rows.getJSONObject(index)
                    val values = ContentValues()
                    objectRow.keys().forEach { key ->
                        when (val value = objectRow.opt(key)) {
                            null, JSONObject.NULL -> values.putNull(key)
                            is Int -> values.put(key, value)
                            is Long -> values.put(key, value)
                            is Double -> values.put(key, value)
                            else -> values.put(key, value.toString())
                        }
                    }
                    db.insertOrThrow(table, null, values)
                }
            }
            updateProfileInTransaction(db, "avatar", "")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun recomputeRoll(db: SQLiteDatabase, rollId: String) {
        val shot = db.rawQuery(
            "SELECT COALESCE(SUM(count), 0) AS total FROM shots WHERE roll_id = ?",
            arrayOf(rollId),
        ).use { if (it.moveToFirst()) it.int("total") else 0 }
        db.rawQuery(
            "SELECT frames, lab_status, lab_store, lab_date, lab_scanned FROM rolls WHERE id = ?",
            arrayOf(rollId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return
            val lab = LabRecord(
                status = labStatusOf(cursor.string("lab_status")),
                store = cursor.string("lab_store"),
                date = cursor.string("lab_date"),
                scanned = cursor.int("lab_scanned") == 1,
            )
            db.update("rolls", ContentValues().apply {
                put("shot", shot)
                put("status", computeStatus(shot, cursor.int("frames"), lab).name)
            }, "id = ?", arrayOf(rollId))
        }
    }

    private fun loadTags(db: SQLiteDatabase): List<TagItem> = buildList {
        db.rawQuery(
            """
            SELECT * FROM tags
            ORDER BY CASE category WHEN 'brand' THEN 0 WHEN 'type' THEN 1 ELSE 2 END, rowid
            """.trimIndent(),
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) add(
                TagItem(
                    id = cursor.string("id"),
                    name = cursor.string("name"),
                    category = tagCategoryOf(cursor.string("category")),
                ),
            )
        }
    }

    private fun loadShots(db: SQLiteDatabase, rollId: String): List<Shot> = buildList {
        db.rawQuery("SELECT * FROM shots WHERE roll_id = ? ORDER BY date", arrayOf(rollId)).use { cursor ->
            while (cursor.moveToNext()) add(
                Shot(
                    id = cursor.string("id"),
                    date = cursor.string("date"),
                    count = cursor.int("count"),
                    note = cursor.string("note"),
                    samples = jsonStrings(cursor.string("samples")),
                ),
            )
        }
    }

    private fun addRollTag(db: SQLiteDatabase, rollId: String, tagId: String) {
        db.insertWithOnConflict("roll_tags", null, ContentValues().apply {
            put("roll_id", rollId)
            put("tag_id", tagId)
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun profileValue(db: SQLiteDatabase, key: String, fallback: String): String =
        db.rawQuery("SELECT value FROM profile WHERE key = ?", arrayOf(key)).use {
            if (it.moveToFirst()) it.string("value") else fallback
        }

    private fun tableToJson(db: SQLiteDatabase, table: String, excludeAvatar: Boolean): JSONArray {
        val array = JSONArray()
        db.rawQuery("SELECT * FROM $table", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (excludeAvatar && cursor.string("key") == "avatar") continue
                val row = JSONObject()
                for (column in cursor.columnNames) {
                    when (cursor.getType(cursor.getColumnIndexOrThrow(column))) {
                        Cursor.FIELD_TYPE_NULL -> row.put(column, JSONObject.NULL)
                        Cursor.FIELD_TYPE_INTEGER -> row.put(column, cursor.getLong(cursor.getColumnIndexOrThrow(column)))
                        Cursor.FIELD_TYPE_FLOAT -> row.put(column, cursor.getDouble(cursor.getColumnIndexOrThrow(column)))
                        else -> row.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)))
                    }
                }
                array.put(row)
            }
        }
        return array
    }

    private fun updateProfileInTransaction(db: SQLiteDatabase, key: String, value: String) {
        db.insertWithOnConflict("profile", null, ContentValues().apply {
            put("key", key)
            put("value", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun seed(db: SQLiteDatabase) {
        SEED_TAGS.forEach { tag ->
            db.insert("tags", null, ContentValues().apply {
                put("id", tag.id)
                put("name", tag.name)
                put("category", tag.category.value)
            })
        }
        SEED_ROLLS.forEach { seed ->
            db.insert("rolls", null, ContentValues().apply {
                put("id", seed.id)
                put("name", seed.name)
                put("brand", seed.brand)
                put("brand_name", seed.brandName)
                put("type", seed.type)
                put("iso", seed.iso)
                put("frames", seed.frames)
                put("shot", seed.shot)
                put("status", seed.status.name)
                put("archived", if (seed.archived) 1 else 0)
                put("archived_date", seed.archivedDate)
                put("custom_tags", "[]")
                put("lab_status", seed.lab.status.value)
                put("lab_store", seed.lab.store)
                put("lab_date", seed.lab.date)
                put("lab_scanned", if (seed.lab.scanned) 1 else 0)
            })
            seed.tagIds.forEach { addRollTag(db, seed.id, it) }
            seed.shots.forEach { shot ->
                db.insert("shots", null, ContentValues().apply {
                    put("id", shot.id)
                    put("roll_id", seed.id)
                    put("date", shot.date)
                    put("count", shot.count)
                    put("note", shot.note)
                    put("samples", "[]")
                })
            }
        }
        updateProfileInTransaction(db, "name", "胶片爱好者")
        updateProfileInTransaction(db, "avatar", "")
    }

    companion object {
        private const val DATABASE_NAME = "filmvault.db"
        private const val DATABASE_VERSION = 1
        private val BACKUP_TABLES = listOf("rolls", "tags", "shots", "roll_tags", "profile")
    }
}

private data class SeedRoll(
    val id: String,
    val name: String,
    val brand: String,
    val brandName: String,
    val type: String,
    val iso: String,
    val frames: Int,
    val shot: Int,
    val status: RollStatus,
    val archived: Boolean,
    val archivedDate: String,
    val tagIds: List<String>,
    val shots: List<Shot>,
    val lab: LabRecord,
)

private val SEED_TAGS = listOf(
    TagItem("tag-brand-kodak", "柯达", TagCategory.BRAND),
    TagItem("tag-brand-fuji", "富士", TagCategory.BRAND),
    TagItem("tag-brand-ilford", "伊尔福", TagCategory.BRAND),
    TagItem("tag-brand-cinestill", "CineStill", TagCategory.BRAND),
    TagItem("tag-type-color", "彩色负片", TagCategory.TYPE),
    TagItem("tag-type-bw", "黑白负片", TagCategory.TYPE),
    TagItem("tag-type-movie", "电影卷", TagCategory.TYPE),
    TagItem("tag-proj-travel", "旅行", TagCategory.PROJECT),
    TagItem("tag-proj-portrait", "人像", TagCategory.PROJECT),
    TagItem("tag-proj-street", "街拍", TagCategory.PROJECT),
)

private val SEED_ROLLS = listOf(
    SeedRoll(
        "kodak-portra-400", "Kodak Portra 400", "柯达", "柯达", "彩色负片", "ISO 400", 36, 18,
        RollStatus.SHOOT, false, "", listOf("tag-brand-kodak", "tag-type-color", "tag-proj-travel"),
        listOf(
            Shot("seed-k-1", "2026.07.12", 8, "周末和朋友去了海边，用了曝光补偿 +1。", emptyList()),
            Shot("seed-k-2", "2026.07.20", 10, "城市夜景，三脚架长曝光。", emptyList()),
        ),
        LabRecord(LabStatus.WASH, "未填写", "—", false),
    ),
    SeedRoll(
        "fujifilm-pro-400h", "Fujifilm Pro 400H", "富士", "富士", "彩色负片", "ISO 400", 36, 36,
        RollStatus.DONE, true, "2026.07.10",
        listOf("tag-brand-fuji", "tag-type-color", "tag-proj-travel", "tag-proj-portrait"),
        listOf(
            Shot("seed-f-1", "2026.06.28", 8, "海边日光，过曝半档更有空气感。", emptyList()),
            Shot("seed-f-2", "2026.07.05", 12, "城市扫街一整天，高光层次很棒。", emptyList()),
            Shot("seed-f-3", "2026.07.10", 16, "最后一卷，人像收尾，圆满。", emptyList()),
        ),
        LabRecord(LabStatus.SENT, "某某胶片冲洗", "2026.07.12", true),
    ),
    SeedRoll(
        "ilford-hp5", "Ilford HP5 Plus", "伊尔福", "伊尔福", "黑白负片", "ISO 400", 24, 24,
        RollStatus.WASH, true, "2026.07.18", listOf("tag-brand-ilford", "tag-type-bw", "tag-proj-street"),
        listOf(
            Shot("seed-i-1", "2026.07.15", 12, "阴天街拍，颗粒感很迷人。", emptyList()),
            Shot("seed-i-2", "2026.07.18", 12, "傍晚收尾，黑白对比强烈。", emptyList()),
        ),
        LabRecord(LabStatus.WASH, "未填写", "—", false),
    ),
    SeedRoll(
        "cinestill-800t", "CineStill 800T", "CineStill", "CineStill", "电影卷", "ISO 800", 36, 0,
        RollStatus.UNSHOT, false, "", listOf("tag-brand-cinestill", "tag-type-movie"), emptyList(), LabRecord(),
    ),
)

private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column)) ?: ""
private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

private fun stringsJson(values: List<String>): String = JSONArray().apply {
    values.distinct().forEach(::put)
}.toString()

private fun jsonStrings(raw: String): List<String> = runCatching {
    val array = JSONArray(raw)
    List(array.length()) { index -> array.optString(index) }.filter(String::isNotEmpty)
}.getOrDefault(emptyList())

private fun rollStatusOf(value: String): RollStatus =
    RollStatus.entries.firstOrNull { it.name == value } ?: RollStatus.UNSHOT

private fun labStatusOf(value: String): LabStatus =
    LabStatus.entries.firstOrNull { it.value == value } ?: LabStatus.NONE

private fun tagCategoryOf(value: String): TagCategory =
    TagCategory.entries.firstOrNull { it.value == value } ?: TagCategory.PROJECT
