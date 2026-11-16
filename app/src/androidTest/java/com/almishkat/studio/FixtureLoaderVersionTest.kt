package com.almishkat.studio

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * ADM-M1-037 — verifies exactly the ten SQL fixtures published in M1-TST-01.
 * v1..v9 are RECONSTRUCTED; v10 is DERIVED FROM CURRENT ONCREATE.
 * Passing these loader tests does NOT authenticate historical SQLite schemas.
 * These tests do not call production migrations.
 */
@RunWith(AndroidJUnit4::class)
class FixtureLoaderVersionTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val tests = InstrumentationRegistry.getInstrumentation().context

    private fun verifyFixture(version: Int) {
        val source = tests.assets.open("db/v" + version + ".sql")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (version < 10) {
            assertTrue("v" + version + " provenance must be RECONSTRUCTED", source.contains("RECONSTRUCTED"))
        } else {
            assertTrue("v10 provenance must be DERIVED FROM CURRENT", source.contains("DERIVED FROM CURRENT"))
        }
        assertTrue("v" + version + " must declare SQLite user_version",
            Regex("PRAGMA\\s+user_version\\s*=\\s*" + version + "\\s*;",RegexOption.IGNORE_CASE).containsMatchIn(source))

        val directory = File(target.cacheDir, "M1-TST-01-LOAD/" + UUID.randomUUID().toString())
        assertTrue("Could not create isolated fixture folder", directory.mkdirs())
        val file = File(directory, "fixture-v" + version + ".db")
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            db.beginTransaction()
            try {
                source.lineSequence().filterNot { it.trimStart().startsWith("--") }
                    .joinToString("\n").split(';')
                    .map { it.trim() }.filter { it.isNotEmpty() }
                    .forEach { db.execSQL(it) }
                assertEquals("v" + version + " declared user_version differs from actual DB",version,db.version)
                val expected = mutableListOf("quran_ayah_hafs","quran_ayah_warsh","quran_dataset_manifest")
                if(version >= 2) expected.add("ayah_timings")
                if(version >= 3) expected.add("studio_projects")
                if(version >= 4) expected.add("audio_clip_selections")
                if(version >= 6) expected.add("scene_layers")
                if(version >= 7) expected.add("scene_backgrounds")
                if(version >= 9) expected.add("studio_scenes")
                val observed = mutableSetOf<String>()
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",null).use { c ->
                    while(c.moveToNext()) observed.add(c.getString(0))
                }
                assertTrue("v" + version + " missing expected tables: " + expected.filterNot { it in observed },
                    observed.containsAll(expected))
                db.rawQuery("PRAGMA integrity_check",null).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("v" + version + " integrity_check", "ok", c.getString(0))
                }
                db.rawQuery("PRAGMA foreign_key_check",null).use { c ->
                    assertEquals("v" + version + " foreign_key_check count",0,c.count)
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        SQLiteDatabase.openDatabase(file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals("v" + version + " reopened user_version differs",version,db.version)
        }
    }

    @Test fun fixture_v01_loads_and_reopens() = verifyFixture(1)
    @Test fun fixture_v02_loads_and_reopens() = verifyFixture(2)
    @Test fun fixture_v03_loads_and_reopens() = verifyFixture(3)
    @Test fun fixture_v04_loads_and_reopens() = verifyFixture(4)
    @Test fun fixture_v05_loads_and_reopens() = verifyFixture(5)
    @Test fun fixture_v06_loads_and_reopens() = verifyFixture(6)
    @Test fun fixture_v07_loads_and_reopens() = verifyFixture(7)
    @Test fun fixture_v08_loads_and_reopens() = verifyFixture(8)
    @Test fun fixture_v09_loads_and_reopens() = verifyFixture(9)
    @Test fun fixture_v10_loads_and_reopens() = verifyFixture(10)
}
