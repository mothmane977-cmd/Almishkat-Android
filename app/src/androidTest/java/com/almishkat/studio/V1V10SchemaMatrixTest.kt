package com.almishkat.studio

import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.Locale

/**
 * M1-TST-01 candidate matrix. NOT accepted historical evidence.
 * v1..v9 = RECONSTRUCTED. v10 = DERIVED FROM CURRENT ONCREATE.
 * Synthetic non-Quran strings only. All tests use isolated cache databases.
 * D02 and the missing pre-v9 studio_scenes migration are deliberately NOT patched.
 */
@RunWith(AndroidJUnit4::class)
class V1V10SchemaMatrixTest {
    private val app: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext: Context get() = InstrumentationRegistry.getInstrumentation().context

    private class Isolated(base: Context, private val backing: File) : ContextWrapper(base) {
        override fun getDatabasePath(name: String): File = backing
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(backing, factory)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openDatabase(backing.absolutePath, factory,
                SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler)
    }

    private data class State(
        val version: Int,
        val rows: Map<String, List<Map<String, String>>>,
        val schema: Map<String, String>,
        val structural: StructuralSchema.Model
    )
    private data class Case(val version: Int, val file: File, val ctx: Context, val old: State)

    private fun newDb(): File {
        val directory = File(app.cacheDir, "M1-TST-01/" + UUID.randomUUID())
        check(directory.mkdirs())
        return File(directory, "almishkat_quran.db")
    }

    private fun sqlFixture(version: Int): String {
        val text = testContext.assets.open("db/v$version.sql")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (version < 10) {
            assertTrue("Missing RECONSTRUCTED provenance on v$version", text.contains("RECONSTRUCTED"))
        } else {
            assertTrue("v10 must be derived from source, not declared original",
                text.contains("DERIVED FROM CURRENT"))
        }
        return text.lineSequence()
            .filterNot { it.trimStart().startsWith("--") }
            .joinToString("\n")
    }

    private fun seed(version: Int): Case {
        require(version in 1..10)
        val file = newDb()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.beginTransaction()
            try {
                sqlFixture(version).split(';').map { it.trim() }
                    .filter { it.isNotEmpty() }.forEach(db::execSQL)
                assertEquals("Fixture PRAGMA user_version mismatch", version, db.version)
                db.execSQL("INSERT INTO quran_ayah_hafs (surah_no,ayah_no,text_uthmani,juz_no,page_no) VALUES (1,1,'TEST_ONLY_NOT_QURAN_HAFS',1,1)")
                db.execSQL("INSERT INTO quran_ayah_warsh (surah_no,ayah_no,text_uthmani,juz_no,page_no) VALUES (1,1,'TEST_ONLY_NOT_QURAN_WARSH',1,1)")
                db.execSQL("INSERT INTO quran_dataset_manifest VALUES ('HAFS','TEST_ONLY','0','test://quran','SHA_TEST',1,30)")
                if (version >= 2) db.execSQL("INSERT INTO ayah_timings VALUES ('HAFS',1,1,100,900,'test://audio',31)")
                if (version >= 3) db.execSQL("INSERT INTO studio_projects VALUES ('p1','TEST','HAFS',1,'emerald','9:16','test://audio',31,32)")
                if (version >= 4) {
                    db.execSQL("INSERT INTO audio_clip_selections (clip_id,source_uri,start_ms,end_ms,created_at,updated_at) VALUES ('clip1','test://audio',120,800,33,34)")
                    if (version >= 5) {
                        db.execSQL("UPDATE audio_clip_selections SET project_id='p1', riwayah='HAFS',surah_no=1,ayah_no=1 WHERE clip_id='clip1'")
                    }
                }
                if (version >= 6) {
                    db.execSQL("INSERT INTO scene_layers (layer_id,project_id,layer_type,title,content,start_ms,end_ms,position_x,position_y,font_size,visible,z_order,created_at,updated_at) VALUES ('layer1','p1','TITLE','TEST','TEST_ONLY',100,300,0.1,0.2,20.0,1,1,35,36)")
                }
                if (version >= 7) {
                    db.execSQL("INSERT INTO scene_backgrounds (project_id,image_uri,preset,motion_enabled,updated_at) VALUES ('p1','test://image','emerald',1,37)")
                }
                if (version >= 9) {
                    db.execSQL("INSERT INTO studio_scenes (scene_id,project_id,title,scene_order,duration_ms,created_at,updated_at) VALUES ('scene1','p1','TEST',1,3000,38,39)")
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        return Case(version, file, Isolated(app, file), snapshot(file))
    }

    private fun typed(c: Cursor, i: Int): String = when(c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> "NULL:"
        Cursor.FIELD_TYPE_INTEGER -> "INT:" + c.getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> "FLOAT:" + c.getDouble(i)
        Cursor.FIELD_TYPE_BLOB -> "BLOB:" +
            android.util.Base64.encodeToString(c.getBlob(i), android.util.Base64.NO_WRAP)
        else -> "TEXT:" + c.getString(i)
    }

    private fun normalSql(sql: String?): String =
        (sql ?: "<NULL>").replace("\"", "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s*([(),])\\s*"), "$1")
            .replace(Regex("\\bIF\\s+NOT\\s+EXISTS\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ").trim().uppercase(Locale.US)

    private fun snapshot(file: File): State = SQLiteDatabase.openDatabase(
        file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        val structures = linkedMapOf<String, String>()
        db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name", null).use { c ->
            while (c.moveToNext()) {
                val key = c.getString(0) + "|" + c.getString(1) + "|" + c.getString(2)
                structures[key] = normalSql(if (c.isNull(3)) null else c.getString(3))
            }
        }
        val data = linkedMapOf<String, List<Map<String, String>>>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                val all = mutableListOf<Map<String, String>>()
                val quoted = "\"" + name.replace("\"", "\"\"") + "\""
                db.rawQuery("SELECT * FROM $quoted", null).use { rows ->
                    while (rows.moveToNext()) {
                        all += (0 until rows.columnCount).associate { j ->
                            rows.getColumnName(j) to typed(rows,j)
                        }
                    }
                }
                data[name] = all.sortedBy { row ->
                    row.toSortedMap().entries.joinToString("|") { it.key + "=" + it.value }
                }
            }
        }
        State(db.version, data, structures, StructuralSchema.capture(db))
    }

    private fun assertPreserves(old: State, new: State) {
        for ((table, rows) in old.rows) {
            val migrated = new.rows[table] ?: throw AssertionError("Lost table $table")
            assertEquals("Different row count in $table", rows.size, migrated.size)
            val originalFields = rows.firstOrNull()?.keys ?: emptySet()
            val before = rows.map { row -> originalFields.sorted().joinToString("|") { k -> k + "=" + row.getValue(k) } }.sorted()
            val after = migrated.map { row -> originalFields.sorted().joinToString("|") { k ->
                k + "=" + (row[k] ?: throw AssertionError("Lost $table.$k"))
            } }.sorted()
            assertEquals("Changed data in $table", before, after)
        }
    }

    private fun validateIntegrity(file: File) {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA integrity_check", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("SQLite integrity_check must be ok", "ok", c.getString(0))
            }
            db.rawQuery("PRAGMA foreign_key_check", null).use { c -> assertEquals(0, c.count) }
            if (db.version == 10) {
                // Logical links may exist WITHOUT declared FOREIGN KEY constraints.
                for (table in listOf("audio_clip_selections", "scene_layers", "scene_backgrounds", "studio_scenes")) {
                    db.rawQuery(
                        "SELECT COUNT(*) FROM $table AS child LEFT JOIN studio_projects AS parent ON child.project_id=parent.project_id WHERE child.project_id IS NOT NULL AND parent.project_id IS NULL",
                        null
                    ).use { c ->
                        assertTrue(c.moveToFirst())
                        assertEquals("Orphan logical project link in $table", 0, c.getInt(0))
                    }
                }
            }
        }
    }

    private fun openViaProduction(ctx: Context) {
        val helper = QuranLocalDatabase(ctx)
        try { helper.writableDatabase } finally { helper.close() }
    }

    private fun canonicalFresh(): State {
        val file = newDb()
        openViaProduction(Isolated(app, file))
        return snapshot(file)
    }

    private fun testUpgrade(version: Int) {
        val candidate = seed(version)
        validateIntegrity(candidate.file)
        openViaProduction(candidate.ctx)
        val after = snapshot(candidate.file)
        assertEquals("user_version after upgrade", 10, after.version)
        assertPreserves(candidate.old, after)
        val fresh = canonicalFresh()
        // Preserve and report original SQLite DDL differences, but don't treat safe
        // physical column order changes as lost-data defects.
        StructuralSchema.logRawDifferences("ADM_M1_028_DDL", fresh.structural, after.structural)
        assertEquals("sqlite_master structural mismatch (objects, columns, indexes, FK)",
            fresh.structural.semantic(), after.structural.semantic())
        validateIntegrity(candidate.file)
        repeat(2) {
            openViaProduction(candidate.ctx)
            assertEquals("Reopening unexpectedly changed schema or data", after, snapshot(candidate.file))
        }
    }

    @Test fun fixture_user_version_1_through_10_verified() {
        for (v in 1..10) assertEquals(v, seed(v).old.version)
    }

    @Test fun migrate_v01_to_v10() = testUpgrade(1)
    @Test fun migrate_v02_to_v10() = testUpgrade(2)
    @Test fun migrate_v03_to_v10() = testUpgrade(3)
    @Test fun migrate_v04_to_v10() = testUpgrade(4)
    @Test fun migrate_v05_to_v10() = testUpgrade(5)
    @Test fun migrate_v06_to_v10() = testUpgrade(6)
    @Test fun migrate_v07_to_v10() = testUpgrade(7)
    @Test fun migrate_v08_to_v10() = testUpgrade(8)
    @Test fun migrate_v09_to_v10() = testUpgrade(9)
    @Test fun fresh_v10_fixture_matches_new_database() = testUpgrade(10)

    @Test fun failure_injected_on_v8_rolls_back_all_rows_and_version() {
        val c = seed(8)
        val helper = object: SQLiteOpenHelper(c.ctx, "almishkat_quran.db", null, 10) {
            override fun onCreate(db: SQLiteDatabase) {}
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                db.execSQL("ALTER TABLE studio_projects ADD COLUMN TEST_INJECTED TEXT")
                throw SQLiteException("M1_TST_01_INJECTED_ROLLBACK")
            }
        }
        try {
            try { helper.writableDatabase } finally { helper.close() }
            fail("Injected failure must throw")
        } catch (error: SQLiteException) {
            assertTrue(error.message.orEmpty().contains("M1_TST_01_INJECTED_ROLLBACK"))
        }
        assertEquals("Failed migration must be fully atomic", c.old, snapshot(c.file))
        validateIntegrity(c.file)
    }
}
