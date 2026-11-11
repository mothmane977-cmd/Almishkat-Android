package com.almishkat.studio

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ADM-M1-016 — Historical RED evidence, separate from post-fix acceptance.
 * A PASS HERE means ONLY that baseline D01 failure was reproduced with rollback.
 * It does NOT mean DB-001/002/003 acceptance passed.
 * v1-v3 SQL fixtures: RECONSTRUCTED / NOT AUTHENTIC HISTORICAL DDL.
 */
@RunWith(AndroidJUnit4::class)
class D01HistoricalRedEvidenceTest {
    private val fixtures = D01MigrationInstrumentedTest()

    private fun expectsHistoricalD01Failure(version: Int) {
        val fixture = fixtures.seed(version)
        val thrown = try {
            fixtures.openProduction(fixture.ctx)
            null
        } catch (error: SQLiteException) {
            error
        }
        assertNotNull("Baseline v$version must fail before D01 is fixed", thrown)
        assertTrue("Expected duplicate project_id evidence for v$version, got: " + thrown?.message,
            thrown?.message.orEmpty().contains("duplicate column name", ignoreCase = true) &&
            thrown?.message.orEmpty().contains("project_id", ignoreCase = true))
        assertEquals("Historical failure must leave the complete database intact",
            fixture.before, fixtures.snapshot(fixture.file))
        fixtures.checkIntegrity(fixture.file)
    }

    @Test fun historical_RED_v1_duplicate_project_id_and_rollback() = expectsHistoricalD01Failure(1)
    @Test fun historical_RED_v2_duplicate_project_id_and_rollback() = expectsHistoricalD01Failure(2)
    @Test fun historical_RED_v3_duplicate_project_id_and_rollback() = expectsHistoricalD01Failure(3)

    @Test fun injected_test_only_upgrade_failure_rolls_back() {
        val fixture = fixtures.seed(3)
        val injection = object : SQLiteOpenHelper(fixture.ctx, "almishkat_quran.db", null, 10) {
            override fun onCreate(db: SQLiteDatabase) { }
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                db.execSQL("ALTER TABLE studio_projects ADD COLUMN test_injected TEXT")
                throw SQLiteException("TEST_ONLY_INJECTED_FAILURE")
            }
        }
        val caught = try {
            try { injection.writableDatabase } finally { injection.close() }
            null
        } catch (error: SQLiteException) {
            error
        }
        assertNotNull("Injected failure must throw", caught)
        assertTrue(caught?.message.orEmpty().contains("TEST_ONLY_INJECTED_FAILURE"))
        assertEquals(fixture.before, fixtures.snapshot(fixture.file))
        fixtures.checkIntegrity(fixture.file)
    }

    @Test fun snapshots_ignore_rowid_and_iteration_order() {
        val fixture = fixtures.seed(3)
        val database = SQLiteDatabase.openDatabase(
            fixture.file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        database.use { db ->
            db.execSQL("INSERT INTO quran_ayah_hafs VALUES (1,2,'TEST_ONLY_B',1,1)")
            db.execSQL("INSERT INTO quran_ayah_hafs VALUES (1,3,'TEST_ONLY_C',1,1)")
        }
        val first = fixtures.snapshot(fixture.file)
        val reorder = SQLiteDatabase.openDatabase(
            fixture.file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        reorder.use { db ->
            db.execSQL("DELETE FROM quran_ayah_hafs WHERE surah_no=1 AND ayah_no IN (2,3)")
            db.execSQL("INSERT INTO quran_ayah_hafs VALUES (1,3,'TEST_ONLY_C',1,1)")
            db.execSQL("INSERT INTO quran_ayah_hafs VALUES (1,2,'TEST_ONLY_B',1,1)")
        }
        assertEquals("Same primary keys and values must compare equal even when rowids differ",
            first, fixtures.snapshot(fixture.file))
        fixtures.checkIntegrity(fixture.file)
    }
}
