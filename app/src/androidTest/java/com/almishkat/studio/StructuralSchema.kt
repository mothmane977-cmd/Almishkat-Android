package com.almishkat.studio

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.util.Locale

/**
 * ADM-M1-028 test-only structural comparator.
 * Compares table/index/column/foreign key semantics; ignores physical column order.
 * Raw DDL is preserved and logged separately, not discarded or called equivalent.
 * CHECK, collation, expression indexes and triggers/views need separate review.
 */
internal object StructuralSchema {
    data class Column(val name: String, val type: String, val notNull: Int,
                      val defaultSql: String?, val primaryKeyOrder: Int)
    data class Index(val key: String, val unique: Int, val origin: String,
                     val partial: Int, val columnNames: List<String?>)
    data class Model(val objects: List<String>,
                     val columns: Map<String, List<Column>>,
                     val indexes: Map<String, List<Index>>,
                     val foreignKeys: Map<String, List<String>>,
                     val rawDdl: Map<String, String?>) {
        fun semantic(): List<Any> = listOf(objects, columns, indexes, foreignKeys)
    }
    private fun quote(name: String): String = "\"" + name.replace("\"", "\"\"") + "\""

    fun capture(db: SQLiteDatabase): Model {
        val objects = mutableListOf<String>()
        val raw = linkedMapOf<String, String?>()
        db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name", null).use { c ->
            while (c.moveToNext()) {
                val key = c.getString(0) + "|" + c.getString(1) + "|" + c.getString(2)
                objects.add(key)
                raw[key] = if (c.isNull(3)) null else c.getString(3)
            }
        }
        val tables = objects.filter { it.startsWith("table|") }.map { it.split('|')[1] }
        val columns = linkedMapOf<String, List<Column>>()
        val indexes = linkedMapOf<String, List<Index>>()
        val foreignKeys = linkedMapOf<String, List<String>>()
        for (table in tables) {
            val cols = mutableListOf<Column>()
            db.rawQuery("PRAGMA table_info(" + quote(table) + ")",null).use { c ->
                while (c.moveToNext()) cols.add(Column(
                    c.getString(1), c.getString(2).trim().uppercase(Locale.US),
                    c.getInt(3), if (c.isNull(4)) null else c.getString(4), c.getInt(5)
                ))
            }
            columns[table] = cols.sortedBy { it.name }
            val idx = mutableListOf<Index>()
            db.rawQuery("PRAGMA index_list(" + quote(table) + ")",null).use { c ->
                while (c.moveToNext()) {
                    val indexName = c.getString(1)
                    val isUnique = c.getInt(2)
                    val originColumn = c.getColumnIndex("origin")
                    val origin = if (originColumn < 0) "UNKNOWN" else c.getString(originColumn)
                    val partialColumn = c.getColumnIndex("partial")
                    val partial = if (partialColumn < 0) -1 else c.getInt(partialColumn)
                    val orderedColumns = mutableListOf<String?>()
                    db.rawQuery("PRAGMA index_info(" + quote(indexName) + ")",null).use { detail ->
                        while (detail.moveToNext()) orderedColumns.add(
                            if (detail.isNull(2)) null else detail.getString(2))
                    }
                    val stable = if (indexName.startsWith("sqlite_autoindex_"))
                        "AUTO:" + origin + ":" + orderedColumns.joinToString(",")
                        else indexName
                    idx.add(Index(stable,isUnique,origin,partial,orderedColumns))
                }
            }
            indexes[table] = idx.sortedBy { it.key }
            val fks = mutableListOf<String>()
            db.rawQuery("PRAGMA foreign_key_list(" + quote(table) + ")",null).use { c ->
                while(c.moveToNext()) fks.add(
                    (0 until c.columnCount).joinToString("|") { i ->
                        if(c.isNull(i)) "<NULL>" else c.getString(i)
                    }
                )
            }
            foreignKeys[table] = fks.sorted()
        }
        return Model(objects.sorted(), columns, indexes, foreignKeys, raw)
    }

    fun logRawDifferences(tag: String, expected: Model, actual: Model) {
        for (key in (expected.rawDdl.keys + actual.rawDdl.keys).toSortedSet()) {
            val left = expected.rawDdl[key]
            val right = actual.rawDdl[key]
            if (left != right) {
                Log.i(tag, "RAW_DDL_DIFFERENCE " + key)
                Log.i(tag, "EXPECTED: " + (left ?: "<ABSENT>"))
                Log.i(tag, "ACTUAL: " + (right ?: "<ABSENT>"))
            }
        }
    }
}
