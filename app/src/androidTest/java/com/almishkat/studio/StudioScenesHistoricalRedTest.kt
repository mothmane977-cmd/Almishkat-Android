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

/**
 * ADM-M1-028: RED evidence for missing studio_scenes on v1-v8 upgrades.
 * Test-only: uses production SQLiteOpenHelper UNMODIFIED; isolated cache DB files.
 * Source v1-v9 SQL fixtures explicitly RECONSTRUCTED; v10 derived, not historical.
 * TEST_ONLY_* are NOT Quran text.
 */
@RunWith(AndroidJUnit4::class)
class StudioScenesHistoricalRedTest {
    private val app: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val tests: Context get() = InstrumentationRegistry.getInstrumentation().context

    private class Sandbox(base: Context, private val f: File): ContextWrapper(base) {
        override fun getDatabasePath(name: String): File = f
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(f, factory)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openDatabase(f.absolutePath, factory, SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler)
    }
    private data class Snapshot(
        val version: Int, val schema: StructuralSchema.Model,
        val rows: Map<String, List<Map<String, String>>>
    )
    private data class Fixture(
        val version: Int, val file: File, val context: Context, val before: Snapshot
    )

    private fun checkedVersion(sql: String, version: Int) {
        assertTrue("Fixture must retain RECONSTRUCTED label v" + version,
            sql.contains("RECONSTRUCTED") && sql.contains("PRAGMA user_version = " + version))
    }
    private fun makeFixture(version: Int, distortion: String = "none"): Fixture {
        require(version in 1..9)
        val directory = File(app.cacheDir, "ADM-M1-028-ONLY/" + UUID.randomUUID())
        assertTrue("Cannot create disposable test directory", directory.mkdirs())
        val file = File(directory,"almishkat_quran.db")
        val raw = tests.assets.open("db/v" + version + ".sql")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        checkedVersion(raw,version)
        var sql = raw.lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
        if (version == 9) {
            when (distortion) {
                "wrong_type" -> {
                    val before = "duration_ms INTEGER NOT NULL"
                    assertTrue(sql.contains(before))
                    sql = sql.replace(before,"duration_ms TEXT NOT NULL")
                }
                "missing_notes" -> {
                    val before = "    notes TEXT NOT NULL DEFAULT '',"
                    assertTrue(sql.contains(before))
                    sql = sql.replace(before,"")
                }
            }
        }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.beginTransaction()
            try {
                sql.split(';').map { it.trim() }.filter { it.isNotEmpty() }.forEach(db::execSQL)
                assertEquals("user_version should match fixture v" + version, version, db.version)
                db.execSQL("INSERT INTO quran_ayah_hafs VALUES (1,1,'TEST_ONLY_HAFS',1,1)")
                db.execSQL("INSERT INTO quran_ayah_warsh VALUES (1,1,'TEST_ONLY_WARSH',1,1)")
                db.execSQL("INSERT INTO quran_dataset_manifest VALUES ('HAFS','TEST', '0','test://source','none',1,1)")
                if (version >= 2) db.execSQL("INSERT INTO ayah_timings VALUES ('HAFS',1,1,30,950,'test://audio',2)")
                if (version >= 3) db.execSQL("INSERT INTO studio_projects VALUES ('p1','TEST','HAFS',1,'emerald','9:16','test://audio',3,4)")
                if (version >= 4) db.execSQL("INSERT INTO audio_clip_selections (clip_id,source_uri,start_ms,end_ms,created_at,updated_at) VALUES ('clip1','test://audio',25,930,5,6)")
                if (version >= 6) db.execSQL("INSERT INTO scene_layers (layer_id,project_id,layer_type,title,content,start_ms,end_ms,position_x,position_y,font_size,visible,z_order,created_at,updated_at) VALUES ('lay1','p1','TITLE','TEST','TEST_ONLY',0,100,0.2,0.3,12,1,0,6,7)")
                if (version >= 7) db.execSQL("INSERT INTO scene_backgrounds (project_id,image_uri,preset,motion_enabled,updated_at) VALUES ('p1','test://image','emerald',1,8)")
                if (version >= 9) {
                    db.execSQL("INSERT INTO studio_scenes (scene_id,project_id,title,scene_order,duration_ms,transition_in,transition_duration_ms,created_at,updated_at) VALUES ('scene-1','p1','FIRST',2,3200,'FADE',220,10,11)")
                    db.execSQL("INSERT INTO studio_scenes (scene_id,project_id,title,scene_order,duration_ms,transition_in,transition_duration_ms,created_at,updated_at) VALUES ('scene-2','p1','SECOND',1,2300,'CUT',0,12,13)")
                    if (distortion == "bad_index") {
                        db.execSQL("DROP INDEX idx_studio_scenes_project_order")
                        db.execSQL("CREATE INDEX idx_studio_scenes_project_order ON studio_scenes(title)")
                    }
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        return Fixture(version, file, Sandbox(app,file), snapshot(file))
    }

    private fun typed(c: Cursor,i:Int):String = when(c.getType(i)) {
        Cursor.FIELD_TYPE_NULL -> "NULL"
        Cursor.FIELD_TYPE_INTEGER -> "INT:" + c.getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> "REAL:" + c.getDouble(i)
        Cursor.FIELD_TYPE_BLOB -> "BLOB:" + android.util.Base64.encodeToString(c.getBlob(i),android.util.Base64.NO_WRAP)
        else -> "TEXT:" + c.getString(i)
    }
    private fun snapshot(file:File):Snapshot =
        SQLiteDatabase.openDatabase(file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            val data = linkedMapOf<String,List<Map<String,String>>>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null).use { t ->
                while (t.moveToNext()) {
                    val name=t.getString(0)
                    val quoted="\"" + name.replace("\"","\"\"") + "\""
                    val items=mutableListOf<Map<String,String>>()
                    db.rawQuery("SELECT * FROM " + quoted,null).use { c ->
                        while(c.moveToNext()) items.add((0 until c.columnCount).associate { i ->
                            c.getColumnName(i) to typed(c,i)
                        })
                    }
                    data[name]=items.sortedBy { item -> item.toSortedMap().entries.joinToString("|") { it.key+"="+it.value } }
                }
            }
            Snapshot(db.version,StructuralSchema.capture(db),data)
        }

    private fun openProduction(ctx:Context) {
        val helper=QuranLocalDatabase(ctx)
        try { helper.writableDatabase } finally { helper.close() }
    }
    private fun containsTable(file:File, name:String):Boolean =
        SQLiteDatabase.openDatabase(file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",arrayOf(name)).use { c ->
                c.moveToFirst();c.getInt(0)>0
            }
        }
    private fun containsIndex(file:File,name:String):Boolean =
        SQLiteDatabase.openDatabase(file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name=?",arrayOf(name)).use { c ->
                c.moveToFirst();c.getInt(0)>0
            }
        }
    private fun checkIntegrity(file:File) =
        SQLiteDatabase.openDatabase(file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA integrity_check",null).use { c ->
                assertTrue(c.moveToFirst());assertEquals("ok",c.getString(0))
            }
            db.rawQuery("PRAGMA foreign_key_check",null).use { c -> assertEquals(0,c.count) }
        }
    private fun assertOriginalRowsKept(old:Snapshot,after:Snapshot) {
        for((table,rows) in old.rows) {
            val newer=after.rows[table]?:throw AssertionError("MISSING_OLD_TABLE:"+table)
            assertEquals("ROW_COUNT_CHANGED:"+table,rows.size,newer.size)
            if(rows.isNotEmpty()) {
                val fields=rows.first().keys.sorted()
                val initial=rows.map { row -> fields.joinToString("|") { field -> field+"="+row.getValue(field) } }.sorted()
                val current=newer.map { row -> fields.joinToString("|") { field -> field+"="+(row[field]?:throw AssertionError("LOST_FIELD:"+table+"."+field)) } }.sorted()
                assertEquals("LOST_OR_CHANGED_USER_DATA:"+table,initial,current)
            }
        }
    }
    private fun v1ToV8Red(version:Int) {
        val candidate=makeFixture(version)
        assertFalse("Invalid test setup: studio_scenes should NOT exist in v" + version,
            containsTable(candidate.file,"studio_scenes"))
        checkIntegrity(candidate.file)
        // If this throws, record as a separate migration failure: do not misclassify it.
        openProduction(candidate.context)
        val after=snapshot(candidate.file)
        assertEquals("Migration did not reach target user_version",10,after.version)
        assertOriginalRowsKept(candidate.before,after)
        checkIntegrity(candidate.file)
        assertTrue("MISSING_STUDIO_SCENES_AFTER_UPGRADE_FROM_v" + version +
            ": onUpgrade lacks createStudioScenesTable", containsTable(candidate.file,"studio_scenes"))
        assertTrue("MISSING_STUDIO_SCENES_INDEX_AFTER_UPGRADE_FROM_v" + version,
            containsIndex(candidate.file,"idx_studio_scenes_project_order"))
    }

    @Test fun red_scene_missing_v1()=v1ToV8Red(1)
    @Test fun red_scene_missing_v2()=v1ToV8Red(2)
    @Test fun red_scene_missing_v3()=v1ToV8Red(3)
    @Test fun red_scene_missing_v4()=v1ToV8Red(4)
    @Test fun red_scene_missing_v5()=v1ToV8Red(5)
    @Test fun red_scene_missing_v6()=v1ToV8Red(6)
    @Test fun red_scene_missing_v7()=v1ToV8Red(7)
    @Test fun red_scene_missing_v8()=v1ToV8Red(8)

    @Test fun control_fresh_install_has_scenes_and_index() {
        val folder=File(app.cacheDir,"ADM-M1-028-FRESH/"+UUID.randomUUID())
        assertTrue(folder.mkdirs())
        val file=File(folder,"almishkat_quran.db")
        openProduction(Sandbox(app,file))
        assertTrue("Production onCreate did not create studio_scenes",containsTable(file,"studio_scenes"))
        assertTrue("Production onCreate did not create scene index",containsIndex(file,"idx_studio_scenes_project_order"))
        checkIntegrity(file)
    }

    @Test fun v9_scenes_two_rows_preserved_and_reopen_twice() {
        val c=makeFixture(9)
        assertTrue(containsTable(c.file,"studio_scenes"))
        assertTrue(containsIndex(c.file,"idx_studio_scenes_project_order"))
        openProduction(c.context)
        val migrated=snapshot(c.file)
        assertEquals(10,migrated.version)
        assertOriginalRowsKept(c.before,migrated)
        checkIntegrity(c.file)
        repeat(2) {
            openProduction(c.context)
            assertEquals("Re-open unexpectedly mutated data or schema",migrated,snapshot(c.file))
        }
        SQLiteDatabase.openDatabase(c.file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT scene_id,scene_order,title,duration_ms,transition_in,transition_duration_ms,notes FROM studio_scenes WHERE project_id=? ORDER BY scene_order",arrayOf("p1")).use { cur ->
                assertEquals(2,cur.count)
                assertTrue(cur.moveToFirst());assertEquals("scene-2",cur.getString(0))
                assertEquals("CUT",cur.getString(4))
                assertTrue(cur.moveToNext());assertEquals("scene-1",cur.getString(0))
                assertEquals(3200L,cur.getLong(3));assertEquals("FADE",cur.getString(4))
                assertEquals(220L,cur.getLong(5))
            }
        }
    }

    private fun incompatibleMustBeRejected(shape:String) {
        val fixture=makeFixture(9,shape)
        val prior=fixture.before
        var exception:SQLiteException?=null
        try { openProduction(fixture.context) } catch(ex:SQLiteException) { exception=ex }
        if(exception==null) {
            val after=snapshot(fixture.file)
            assertOriginalRowsKept(prior,after)
            fail("INCOMPATIBLE_STUDIO_SCENES_ACCEPTED_SILENTLY:"+shape)
        }
        // Do not count a generic unrelated failure as the expected schema check.
        assertTrue("WRONG_REJECTION_CAUSE:"+exception?.message,
            exception?.message.orEmpty().contains("studio_scenes",true))
        assertEquals("INCOMPATIBLE_SCHEMA_REJECTION_MUST_ROLLBACK",prior,snapshot(fixture.file))
        checkIntegrity(fixture.file)
    }

    @Test fun red_v9_wrong_scene_index_requires_rejection()=incompatibleMustBeRejected("bad_index")
    @Test fun red_v9_wrong_duration_type_requires_rejection()=incompatibleMustBeRejected("wrong_type")
    @Test fun red_v9_missing_notes_column_requires_rejection()=incompatibleMustBeRejected("missing_notes")

    @Test fun control_injected_sqliteopenhelper_failure_rolls_back() {
        val c=makeFixture(9)
        val before=c.before
        val helper=object:SQLiteOpenHelper(c.context,"almishkat_quran.db",null,10) {
            override fun onCreate(db:SQLiteDatabase) {}
            override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) {
                db.execSQL("ALTER TABLE studio_scenes ADD COLUMN test_injected TEXT")
                throw SQLiteException("ADM_M1_028_TEST_ONLY_INJECTED")
            }
        }
        try {
            try { helper.writableDatabase } finally { helper.close() }
            fail("Injection did not fail")
        } catch(ex:SQLiteException) {
            assertTrue(ex.message.orEmpty().contains("ADM_M1_028_TEST_ONLY_INJECTED"))
        }
        assertEquals("Injected rollback lost rows or DDL",before,snapshot(c.file))
        assertEquals(9,snapshot(c.file).version)
        checkIntegrity(c.file)
        // Reopen the original database without triggering another upgrade.
        SQLiteDatabase.openDatabase(c.file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(9,db.version)
        }
    }

    @Test fun control_column_order_differs_but_structure_matches_and_raw_ddl_is_saved() {
        val f1=File.createTempFile("adm-m1-028-a",".db",app.cacheDir)
        val f2=File.createTempFile("adm-m1-028-b",".db",app.cacheDir)
        SQLiteDatabase.openOrCreateDatabase(f1,null).use { db ->
            db.execSQL("CREATE TABLE t (id TEXT PRIMARY KEY, alpha TEXT, beta INTEGER)")
        }
        SQLiteDatabase.openOrCreateDatabase(f2,null).use { db ->
            db.execSQL("CREATE TABLE t (beta INTEGER, id TEXT PRIMARY KEY, alpha TEXT)")
        }
        val a=SQLiteDatabase.openDatabase(f1.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { StructuralSchema.capture(it) }
        val b=SQLiteDatabase.openDatabase(f2.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { StructuralSchema.capture(it) }
        assertEquals("Column order must not be treated as structural loss",a.semantic(),b.semantic())
        assertNotEquals("Raw SQL differences must remain visible",a.rawDdl,b.rawDdl)
        StructuralSchema.logRawDifferences("ADM_M1_028_DDL",a,b)
    }
}
