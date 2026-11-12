package com.almishkat.studio

import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class DatabaseRegressionTest : DatabaseRegressionContract() {
    override fun testContext(): Context = RuntimeEnvironment.getApplication()
    override fun fixtureSql(version: Int): String =
        javaClass.getResource("/db/v$version.sql")!!.readText()
}
