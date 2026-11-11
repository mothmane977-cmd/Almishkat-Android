package com.almishkat.studio

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * ADM-M1-012. RED instrumentation against UNMODIFIED production SQLiteOpenHelper.
 * Historical v1-v3 = RECONSTRUCTED; v4/v5 = scenario-only, NOT authentic DDL.
 * All seeded Quran-like strings are TEST_ONLY_NOT_QURAN, never authenticated text.
 * Database files are isolated under cache: no existing user database is touched.
 */
@RunWith(AndroidJUnit4::class)
class D01MigrationInstrumentedTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testAssets: Context get() = InstrumentationRegistry.getInstrumentation().context

    private data class Snapshot(val version: Int, val schema: List<String>,
                                val data: Map<String, List<Map<String, String>>>)
    private data class Fixture(val file: File, val ctx: Context, val before: Snapshot)

    private class IsolatedContext(base: Context, private val file: File) : ContextWrapper(base) {
        override fun getDatabasePath(name: String): File = file
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(file, factory)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openDatabase(file.absolutePath, factory,
                SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler)
    }

    private fun snapshot(file: File): Snapshot =
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val schema = mutableListOf<String>()
            db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name", null).use { c ->
                while (c.moveToNext()) schema.add((0 until c.columnCount).joinToString("|") { value(c,it) })
            }
            val tables = mutableListOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name", null).use { c ->
                while (c.moveToNext()) tables.add(c.getString(0))
            }
            val rows = linkedMapOf<String,List<Map<String,String>>>()
            for (table in tables) {
                val list = mutableListOf<Map<String,String>>()
                db.rawQuery("SELECT * FROM " + table + " ORDER BY rowid", null).use { c ->
                    while (c.moveToNext()) list.add((0 until c.columnCount).associate { c.getColumnName(it) to value(c,it) })
                }
                rows[table] = list
            }
            Snapshot(db.version, schema, rows)
        }

    private fun value(c: Cursor, col: Int): String = when (c.getType(col)) {
        Cursor.FIELD_TYPE_NULL -> "N"
        Cursor.FIELD_TYPE_INTEGER -> "I" + c.getLong(col)
        Cursor.FIELD_TYPE_FLOAT -> "F" + c.getDouble(col)
        Cursor.FIELD_TYPE_BLOB -> "B" + android.util.Base64.encodeToString(c.getBlob(col), android.util.Base64.NO_WRAP)
        else -> "S" + c.getString(col)
    }

    private fun seed(version: Int, shape: String = "standard"): Fixture {
        require(version in 1..5)
        val dir = File(context.cacheDir, "adm-m1-012-ONLY/" + UUID.randomUUID())
        check(dir.mkdirs())
        val file = File(dir, "almishkat_quran.db")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.beginTransaction()
            try {
                val fixtureVersion = minOf(version, 3)
                val sql = testAssets.assets.open("db/v" + fixtureVersion + ".sql")
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
                check("RECONSTRUCTED" in sql)
                sql.lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
                    .split(';').map { it.trim() }.filter { it.isNotBlank() }.forEach(db::execSQL)
                if (version >= 4 && shape != "missing") createAudio(db, version, shape)
                db.execSQL("INSERT INTO quran_ayah_hafs VALUES (1,1,'TEST_ONLY_NOT_QURAN_HAFS',1,1)")
                db.execSQL("INSERT INTO quran_ayah_warsh VALUES (1,1,'TEST_ONLY_NOT_QURAN_WARSH',1,1)")
                db.execSQL("INSERT INTO quran_dataset_manifest VALUES ('HAFS','TEST_ONLY','0','test://source','000',1,10)")
                if (version >= 2) db.execSQL("INSERT INTO ayah_timings VALUES ('HAFS',1,1,10,900,'test://audio',11)")
                if (version >= 3) db.execSQL("INSERT INTO studio_projects VALUES ('p1','TEST_ONLY','HAFS',1,'test','9:16','test://audio',10,11)")
                if (version >= 4 && shape != "missing") {
                    val values = ContentValues().apply {
                        put("clip_id", "clip1"); put("source_uri", "test://audio")
                        put("start_ms", 20); put("end_ms", 800)
                        put("created_at", 10); put("updated_at", 11)
                    }
                    val names = mutableSetOf<String>()
                    db.rawQuery("PRAGMA table_info(audio_clip_selections)", null).use { c ->
                        while (c.moveToNext()) names.add(c.getString(1))
                    }
                    if ("project_id" in names) {
                        if (shape == "wrong_type") values.put("project_id", 1)
                        else values.put("project_id", "p1")
                    }
                    if ("riwayah" in names) values.put("riwayah", "HAFS")
                    if ("surah_no" in names) values.put("surah_no", 1)
                    if ("ayah_no" in names) {
                        if (shape == "wrong_ayah") values.put("ayah_no", "ONE")
                        else values.put("ayah_no", 1)
                    }
                    db.insertOrThrow("audio_clip_selections", null, values)
                }
                db.version = version
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        return Fixture(file, IsolatedContext(context, file), snapshot(file))
    }

    private fun createAudio(db: SQLiteDatabase, version: Int, shape: String) {
        val extra = when (shape) {
            "partial" -> if (version==4) listOf("project_id TEXT","surah_no INTEGER")
                         else listOf("project_id TEXT","riwayah TEXT","surah_no INTEGER")
            "wrong_type" -> listOf("project_id INTEGER")
            "not_null" -> listOf("project_id TEXT NOT NULL DEFAULT 'p1'")
            "wrong_ayah" -> listOf("project_id TEXT","riwayah TEXT","surah_no INTEGER","ayah_no TEXT")
            else -> if (version==4) emptyList() else listOf(
                "project_id TEXT","riwayah TEXT","surah_no INTEGER","ayah_no INTEGER")
        }
        val cols = listOf("clip_id TEXT PRIMARY KEY","source_uri TEXT NOT NULL",
            "start_ms INTEGER NOT NULL","end_ms INTEGER NOT NULL",
            "created_at INTEGER NOT NULL","updated_at INTEGER NOT NULL") + extra
        db.execSQL("CREATE TABLE audio_clip_selections (" + cols.joinToString(",") + ")")
        db.execSQL("CREATE INDEX idx_audio_clip_uri ON audio_clip_selections(source_uri)")
        if (shape=="wrong_index") db.execSQL("CREATE INDEX idx_audio_clip_project ON audio_clip_selections(source_uri)")
        else if (version==5) db.execSQL("CREATE INDEX idx_audio_clip_project ON audio_clip_selections(project_id)")
    }

    private fun checkPreservation(old: Snapshot, now: Snapshot) {
        for ((table, entries) in old.data) {
            val newEntries = now.data[table] ?: throw AssertionError("Lost table: " + table)
            assertEquals("Row count changed in " + table, entries.size, newEntries.size)
            for ((rowIndex, original) in entries.withIndex())
                for ((col, value) in original)
                    assertEquals("Changed " + table + "." + col, value, newEntries[rowIndex][col])
        }
    }

    private fun checkAudio(file: File) {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val cols = linkedMapOf<String,List<String>>()
            db.rawQuery("PRAGMA table_info(audio_clip_selections)", null).use { c ->
                while (c.moveToNext()) cols[c.getString(1)] = listOf(
                    c.getString(2).uppercase(), c.getInt(3).toString(),
                    if (c.isNull(4)) "NULL" else c.getString(4), c.getInt(5).toString())
            }
            for ((col,typ) in mapOf("project_id" to "TEXT","riwayah" to "TEXT",
                "surah_no" to "INTEGER","ayah_no" to "INTEGER"))
                assertEquals("D01 column type/constraints: " + col,
                    listOf(typ,"0","NULL","0"), cols[col])
            for ((col,typ) in mapOf("clip_id" to "TEXT","source_uri" to "TEXT",
                "start_ms" to "INTEGER","end_ms" to "INTEGER",
                "created_at" to "INTEGER","updated_at" to "INTEGER")) {
                val expected = listOf(typ, if (col=="clip_id") "0" else "1", "NULL",
                    if (col=="clip_id") "1" else "0")
                assertEquals("Core audio column changed: " + col, expected, cols[col])
            }
            val indexCols = mutableListOf<String>()
            db.rawQuery("PRAGMA index_info(idx_audio_clip_project)", null).use { c ->
                while (c.moveToNext()) indexCols.add(c.getString(2))
            }
            assertEquals(listOf("project_id"), indexCols)
        }
    }

    private fun checkIntegrity(file: File) {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA integrity_check", null).use { c ->
                assertTrue(c.moveToFirst()); assertEquals("ok", c.getString(0))
            }
            db.rawQuery("PRAGMA foreign_key_check", null).use { c -> assertEquals(0,c.count) }
        }
    }

    // SQLiteOpenHelper is not AutoCloseable on API 26; close explicitly on every API.
    private fun openProduction(ctx: Context) {
        val helper = QuranLocalDatabase(ctx)
        try { helper.writableDatabase } finally { helper.close() }
    }

    private fun upgrade(version: Int, shape: String = "standard", reopen: Boolean = false) {
        val fx = seed(version, shape)
        openProduction(fx.ctx)
        val after = snapshot(fx.file)
        assertEquals(10, after.version)
        checkPreservation(fx.before, after)
        checkAudio(fx.file)
        checkIntegrity(fx.file)
        if (reopen) repeat(2) {
            openProduction(fx.ctx)
            assertEquals("Reopen must be idempotent", after, snapshot(fx.file))
        }
        Log.i("ADM_M1_012", "D01 upgrade PASS v" + version + " " + shape)
    }

    private fun mustRejectAtomically(version: Int, shape: String, reason: String? = null) {
        val fx = seed(version,shape)
        var error: SQLiteException? = null
        try { openProduction(fx.ctx) }
        catch (e: SQLiteException) { error = e }
        if (error==null) fail("Invalid D01 schema was accepted: v" + version + " " + shape)
        assertEquals("Migration error must rollback all state",fx.before,snapshot(fx.file))
        checkIntegrity(fx.file)
        if (reason!=null) assertTrue(
            "The rejection must explain the schema defect, not merely duplicate column: " + error?.message,
            error?.message.orEmpty().contains(reason,ignoreCase=true))
    }

    // These tests SHOULD BE RED on unmodified production, never convert failure to PASS.
    @Test fun DB_001_v1_to_v10() = upgrade(1)
    @Test fun DB_002_v2_to_v10() = upgrade(2)
    @Test fun DB_003_v3_to_v10() = upgrade(3)
    @Test fun v4_standard_upgrade_and_two_reopens() = upgrade(4,reopen=true)
    @Test fun v4_partial_links_upgraded() = upgrade(4,"partial")
    @Test fun v5_standard_upgrade_and_two_reopens() = upgrade(5,reopen=true)
    @Test fun v5_partial_links_detected() = upgrade(5,"partial")
    @Test fun v4_wrong_type_rejected() = mustRejectAtomically(4,"wrong_type","type")
    @Test fun v4_not_null_rejected() = mustRejectAtomically(4,"not_null","constraint")
    @Test fun v4_wrong_index_rejected() = mustRejectAtomically(4,"wrong_index","index")
    @Test fun v4_missing_table_rejected() = mustRejectAtomically(4,"missing")
    @Test fun v5_wrong_ayah_type_rejected() = mustRejectAtomically(5,"wrong_ayah","type")
    @Test fun v5_wrong_index_rejected() = mustRejectAtomically(5,"wrong_index","index")
    @Test fun v5_missing_table_rejected() = mustRejectAtomically(5,"missing")

    // This separate positive test proves test-harness transaction rollback only.
    @Test fun injected_failure_in_test_only_helper_rolls_back() {
        val fx=seed(3)
        val injection=object:SQLiteOpenHelper(fx.ctx,"almishkat_quran.db",null,10) {
            override fun onCreate(db:SQLiteDatabase) { }
            override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) {
                db.execSQL("ALTER TABLE studio_projects ADD COLUMN test_injected TEXT")
                throw SQLiteException("TEST_ONLY_INJECTED_FAILURE")
            }
        }
        try {
            try { injection.writableDatabase } finally { injection.close() }
            fail("Injected failure must throw")
        } catch(e:SQLiteException) {
            assertTrue(e.message.orEmpty().contains("TEST_ONLY_INJECTED_FAILURE"))
        }
        assertEquals(fx.before,snapshot(fx.file))
        checkIntegrity(fx.file)
    }

    @Test fun original_D01_failure_does_not_corrupt_v1_fixture() {
        val fx=seed(1)
        try {
            openProduction(fx.ctx)
            fail("Unexpected baseline success")
        } catch(e:SQLiteException) {
            assertTrue("Must be original duplicate-column failure",
                e.message.orEmpty().contains("duplicate column name"))
        }
        assertEquals(fx.before,snapshot(fx.file))
        checkIntegrity(fx.file)
    }
}
