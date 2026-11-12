package com.almishkat.studio

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Real SQLite via Robolectric, not mocked SQL. See docs/db/M1_REPAIR_EVIDENCE.md.
 * v1-v4 are RECONSTRUCTED scenarios, not authentic historical database snapshots.
 * All text below is synthetic TEST_ONLY data, not Quran content.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class DatabaseRegressionTest {
    private lateinit var context: Context
    private lateinit var helper: QuranLocalDatabase

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("almishkat_quran.db")
        helper = QuranLocalDatabase(context)
    }

    @After fun tearDown() {
        helper.close()
        context.deleteDatabase("almishkat_quran.db")
    }

    private fun fixture(version: Int, missingScenes: Boolean = false) {
        val file = context.getDatabasePath("almishkat_quran.db")
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val sql = javaClass.getResource("/db/v$version.sql")!!.readText()
            sql.lineSequence().filterNot { it.trimStart().startsWith("--") }
                .joinToString("\n").split(';').map(String::trim)
                .filter(String::isNotEmpty).forEach(db::execSQL)
            db.execSQL("INSERT INTO quran_ayah_hafs VALUES (1,1,'TEST_ONLY_HAFS',1,1)")
            db.execSQL("INSERT INTO quran_ayah_warsh VALUES (1,1,'TEST_ONLY_WARSH',1,1)")
            db.execSQL("INSERT INTO quran_dataset_manifest VALUES ('HAFS','TEST_ONLY','0','test://source','000',1,10)")
            if (version >= 2) db.execSQL("INSERT INTO ayah_timings VALUES ('HAFS',1,1,10,900,'test://audio',11)")
            if (version >= 3) db.execSQL("INSERT INTO studio_projects VALUES ('p','TEST_ONLY','HAFS',1,'test','9:16',NULL,10,11)")
            if (version == 4) db.execSQL("INSERT INTO audio_clip_selections VALUES ('clip','test://audio',10,900,10,11)")
            if (version == 10) {
                db.execSQL("INSERT INTO audio_clip_selections VALUES ('clip','test://audio',10,900,'p','HAFS',1,1,10,11)")
                db.execSQL("INSERT INTO scene_layers VALUES ('layer','p','TEXT','TEST_ONLY','TEST_ONLY',0,900,0,0,24,1,0,10,11,'scene')")
                db.execSQL("INSERT INTO scene_backgrounds VALUES ('p','test://image',NULL,NULL,'emerald',1,11)")
                if (missingScenes) db.execSQL("DROP TABLE studio_scenes")
                else db.execSQL("INSERT INTO studio_scenes VALUES ('scene','p','TEST_ONLY',0,900,'CUT',0,'TEST_ONLY',10,11)")
            }
        }
    }

    private fun rows(db: SQLiteDatabase, table: String): List<Map<String, String?>> =
        db.rawQuery("SELECT * FROM $table ORDER BY rowid", null).use { c ->
            buildList {
                while (c.moveToNext()) add((0 until c.columnCount).associate {
                    c.getColumnName(it) to if (c.isNull(it)) null else c.getString(it)
                })
            }
        }

    private fun snapshot(db: SQLiteDatabase): Map<String, List<Map<String, String?>>> {
        val tables = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
            null
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
        return tables.associateWith { rows(db, it) }
    }

    private fun checkIntegrity(db: SQLiteDatabase) {
        db.rawQuery("PRAGMA integrity_check", null).use {
            assertTrue(it.moveToFirst()); assertEquals("ok", it.getString(0))
        }
        db.rawQuery("PRAGMA foreign_key_check", null).use { assertEquals(0, it.count) }
    }

    private fun upgrade(version: Int, missingScenes: Boolean = false) {
        fixture(version, missingScenes)
        val before = SQLiteDatabase.openDatabase(
            context.getDatabasePath("almishkat_quran.db").path, null, SQLiteDatabase.OPEN_READONLY
        ).use(::snapshot)
        val db = helper.writableDatabase
        // The observable contract is that existing data can still be read after opening.
        assertEquals("TEST_ONLY_HAFS", helper.getAyah(QuranRiwayah.HAFS, 1, 1)!!.textUthmani)
        before.forEach { (table, original) ->
            val oldColumns = original.firstOrNull()?.keys.orEmpty()
            assertEquals("Lost data in $table", original,
                rows(db, table).map { row -> row.filterKeys { it in oldColumns } })
        }
        // Scene operations must work for both old upgrades and installed v10 databases.
        helper.saveStudioScene(StudioScene(id = "new", projectId = "p", title = "TEST_ONLY"))
        assertTrue(helper.listStudioScenes("p").any { it.id == "new" })
        assertEquals(11, db.version)
        checkIntegrity(db)
        val after = snapshot(db)
        repeat(2) {
            helper.close()
            helper = QuranLocalDatabase(context)
            assertEquals("Reopen changed data", after, snapshot(helper.writableDatabase))
            checkIntegrity(helper.writableDatabase)
        }
    }

    @Test fun reconstructedV1UpgradePreservesData() = upgrade(1)
    @Test fun reconstructedV2UpgradePreservesData() = upgrade(2)
    @Test fun reconstructedV3UpgradePreservesData() = upgrade(3)
    @Test fun reconstructedV4UpgradePreservesAudio() = upgrade(4)
    @Test fun installedV10MissingScenesIsRepaired() = upgrade(10, missingScenes = true)
    @Test fun installedV10ExistingScenesArePreserved() = upgrade(10)

    @Test fun repeatedGlobalBackgroundSaveUpdatesOneRowAndSurvivesReopen() {
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://old"))
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://new"))
        assertEquals(1, rows(helper.readableDatabase, "scene_backgrounds").size)
        helper.close()
        helper = QuranLocalDatabase(context)
        assertEquals("test://new", helper.getSceneBackground("p")!!.imageUri)
    }

    @Test fun backgroundSaveIsIsolatedByProjectAndScene() {
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://global"))
        helper.saveSceneBackground(SceneBackground("other", imageUri = "test://other"))
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://scene-old", sceneId = "scene"))
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://scene-new", sceneId = "scene"))
        assertEquals(3, rows(helper.readableDatabase, "scene_backgrounds").size)
        assertEquals("test://global", helper.getSceneBackground("p")!!.imageUri)
        assertEquals("test://other", helper.getSceneBackground("other")!!.imageUri)
        assertEquals("test://scene-new", helper.getSceneBackgroundForScene("scene")!!.imageUri)
    }

    private fun duplicateBackgrounds() {
        // Pre-existing duplicates remain intact.
        helper.writableDatabase.execSQL("INSERT INTO scene_backgrounds VALUES ('p','test://old',NULL,NULL,'emerald',1,10)")
        helper.writableDatabase.execSQL("INSERT INTO scene_backgrounds VALUES ('p','test://newest',NULL,NULL,'emerald',1,20)")
    }

    @Test fun legacyDuplicateBackgroundReadUsesNewestTimestamp() {
        duplicateBackgrounds()
        // The most recently inserted row is deliberately older: rowid alone is insufficient.
        helper.writableDatabase.execSQL("INSERT INTO scene_backgrounds VALUES ('p','test://even-older',NULL,NULL,'emerald',1,5)")
        assertEquals("test://newest", helper.getSceneBackground("p")!!.imageUri)
        assertEquals(3, rows(helper.readableDatabase, "scene_backgrounds").size)
    }

    @Test fun legacyDuplicateTimestampTieUsesLastInsertedRow() {
        duplicateBackgrounds()
        helper.writableDatabase.execSQL("UPDATE scene_backgrounds SET updated_at=20")
        assertEquals("test://newest", helper.getSceneBackground("p")!!.imageUri)
    }

    @Test fun savingLegacyDuplicatesDoesNotGrowOrOverwriteHistory() {
        duplicateBackgrounds()
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://saved"))
        val images = rows(helper.readableDatabase, "scene_backgrounds").map { it["image_uri"] }
        assertEquals(2, images.size)
        assertTrue("Preserve older duplicate for a separately reviewed cleanup", "test://old" in images)
        assertEquals("test://saved", helper.getSceneBackground("p")!!.imageUri)
    }

    @Test fun saveWithFutureLegacyTimestampsRemainsLatest() {
        duplicateBackgrounds()
        helper.writableDatabase.execSQL("UPDATE scene_backgrounds SET updated_at=9000000000000")
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://saved"))
        assertEquals(2, rows(helper.readableDatabase, "scene_backgrounds").size)
        assertEquals("test://saved", helper.getSceneBackground("p")!!.imageUri)
    }

    @Test fun backgroundUpdateCanClearMediaAndChangeOptions() {
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://image", videoUri = "test://video"))
        helper.saveSceneBackground(SceneBackground("p", preset = "changed", motionEnabled = false))
        val actual = helper.getSceneBackground("p")!!
        assertNull(actual.imageUri)
        assertNull(actual.videoUri)
        assertEquals("changed", actual.preset)
        assertFalse(actual.motionEnabled)
    }

    @Test fun failedBackgroundUpdateRollsBackAndReleasesTransaction() {
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://original"))
        val db = helper.writableDatabase
        val before = rows(db, "scene_backgrounds")
        db.execSQL("CREATE TRIGGER test_abort_update BEFORE UPDATE ON scene_backgrounds BEGIN SELECT RAISE(ABORT,'TEST_ONLY_ABORT'); END")
        assertThrows(SQLiteException::class.java) {
            helper.saveSceneBackground(SceneBackground("p", imageUri = "test://rejected"))
        }
        assertEquals(before, rows(db, "scene_backgrounds"))
        db.execSQL("DROP TRIGGER test_abort_update")
        helper.saveSceneBackground(SceneBackground("p", imageUri = "test://retry"))
        assertEquals("test://retry", helper.getSceneBackground("p")!!.imageUri)
        assertEquals(1, rows(db, "scene_backgrounds").size)
    }
}
