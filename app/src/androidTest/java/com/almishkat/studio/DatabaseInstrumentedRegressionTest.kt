package com.almishkat.studio

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith

/** Runs exactly the same assertions as DatabaseRegressionTest on Android SQLite. */
@RunWith(AndroidJUnit4::class)
class DatabaseInstrumentedRegressionTest : DatabaseRegressionContract() {
    override fun testContext(): Context = InstrumentationRegistry.getInstrumentation().targetContext
    override fun fixtureSql(version: Int): String =
        InstrumentationRegistry.getInstrumentation().context.assets.open("db/v$version.sql")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
}
