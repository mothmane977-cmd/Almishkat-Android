package com.almishkat.studio

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.security.MessageDigest

data class QuranAyahRecord(
    val riwayah: QuranRiwayah,
    val surahNumber: Int,
    val ayahNumber: Int,
    val textUthmani: String,
    val juzNumber: Int? = null,
    val pageNumber: Int? = null
)

data class QuranDatasetManifest(
    val riwayah: QuranRiwayah,
    val sourceName: String,
    val sourceVersion: String,
    val expectedSha256: String,
    val sourceUrl: String
)

data class QuranImportReport(
    val riwayah: QuranRiwayah,
    val importedAyahs: Int,
    val sha256: String,
    val sourceVersion: String
)

class QuranDatasetException(message: String) : Exception(message)

/** Offline-first SQLite storage with physically separate tables per riwayah. */
class QuranLocalDatabase(context: Context) :
    SQLiteOpenHelper(context, "almishkat_quran.db", null, 11) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE quran_ayah_hafs (
                surah_no INTEGER NOT NULL,
                ayah_no INTEGER NOT NULL,
                text_uthmani TEXT NOT NULL,
                juz_no INTEGER,
                page_no INTEGER,
                PRIMARY KEY(surah_no, ayah_no)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE quran_ayah_warsh (
                surah_no INTEGER NOT NULL,
                ayah_no INTEGER NOT NULL,
                text_uthmani TEXT NOT NULL,
                juz_no INTEGER,
                page_no INTEGER,
                PRIMARY KEY(surah_no, ayah_no)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE quran_dataset_manifest (
                riwayah TEXT PRIMARY KEY,
                source_name TEXT NOT NULL,
                source_version TEXT NOT NULL,
                source_url TEXT NOT NULL,
                sha256 TEXT NOT NULL,
                ayah_count INTEGER NOT NULL,
                imported_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_hafs_surah ON quran_ayah_hafs(surah_no)")
        db.execSQL("CREATE INDEX idx_warsh_surah ON quran_ayah_warsh(surah_no)")
        createTimingTable(db)
        createProjectsTable(db)
        createAudioClipTable(db)
        createSceneLayersTable(db)
        createSceneBackgroundTable(db)
        createStudioScenesTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createTimingTable(db)
        if (oldVersion < 3) createProjectsTable(db)
        if (oldVersion < 4) createAudioClipTable(db)
        if (oldVersion < 5) migrateAudioClipLinks(db)
        if (oldVersion < 6) createSceneLayersTable(db)
        if (oldVersion < 7) createSceneBackgroundTable(db)
        if (oldVersion == 7) db.execSQL("ALTER TABLE scene_backgrounds ADD COLUMN video_uri TEXT")
        if (oldVersion < 10) {
            if (oldVersion >= 6) db.execSQL("ALTER TABLE scene_layers ADD COLUMN scene_id TEXT")
            if (oldVersion >= 7) db.execSQL("ALTER TABLE scene_backgrounds ADD COLUMN scene_id TEXT")
            db.execSQL("""
                CREATE TABLE scene_backgrounds_v10 (
                    project_id TEXT NOT NULL,
                    image_uri TEXT,
                    video_uri TEXT,
                    scene_id TEXT,
                    preset TEXT NOT NULL DEFAULT 'emerald',
                    motion_enabled INTEGER NOT NULL DEFAULT 1,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(project_id, scene_id)
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO scene_backgrounds_v10
                (project_id,image_uri,video_uri,scene_id,preset,motion_enabled,updated_at)
                SELECT project_id,image_uri,video_uri,scene_id,preset,motion_enabled,updated_at
                FROM scene_backgrounds
            """.trimIndent())
            db.execSQL("DROP TABLE scene_backgrounds")
            db.execSQL("ALTER TABLE scene_backgrounds_v10 RENAME TO scene_backgrounds")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_scene_layers_scene ON scene_layers(scene_id, z_order)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_scene_backgrounds_scene ON scene_backgrounds(scene_id)")
        }
        // v10 upgrades could finish without creating the scene table. A version
        // bump repairs those installed databases as well as older upgrade paths.
        if (oldVersion < 11) createStudioScenesTable(db)
        // Explicit migrations only. Never drop Quran data during an upgrade.
    }

    private fun createProjectsTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS studio_projects (
                project_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                riwayah TEXT NOT NULL,
                surah_no INTEGER NOT NULL,
                theme_id TEXT NOT NULL,
                canvas_ratio TEXT NOT NULL,
                audio_uri TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
    }


    private fun createAudioClipTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS audio_clip_selections (
                clip_id TEXT PRIMARY KEY,
                source_uri TEXT NOT NULL,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                project_id TEXT,
                riwayah TEXT,
                surah_no INTEGER,
                ayah_no INTEGER,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_audio_clip_uri ON audio_clip_selections(source_uri)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_audio_clip_project ON audio_clip_selections(project_id)")
    }

    private fun migrateAudioClipLinks(db: SQLiteDatabase) {
        // v1-v3 have just created the complete table above. Existing v4 tables
        // may still need these nullable columns; never add the same field twice.
        val existing = db.rawQuery("PRAGMA table_info(audio_clip_selections)", null).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(1).lowercase(java.util.Locale.ROOT)) }
        }
        for ((name, type) in mapOf(
            "project_id" to "TEXT", "riwayah" to "TEXT",
            "surah_no" to "INTEGER", "ayah_no" to "INTEGER"
        )) {
            if (name !in existing) {
                db.execSQL("ALTER TABLE audio_clip_selections ADD COLUMN $name $type")
            }
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_audio_clip_project ON audio_clip_selections(project_id)")
    }

    fun saveAudioClipSelection(selection: AudioClipSelection) {
        val values = ContentValues().apply {
            put("clip_id", selection.id)
            put("source_uri", selection.sourceUri)
            put("start_ms", selection.startMs)
            put("end_ms", selection.endMs)
            put("project_id", selection.projectId)
            put("riwayah", selection.riwayah?.name)
            selection.surahNumber?.let { put("surah_no", it) }
            selection.ayahNumber?.let { put("ayah_no", it) }
            put("created_at", selection.createdAt)
            put("updated_at", selection.updatedAt)
        }
        writableDatabase.insertWithOnConflict(
            "audio_clip_selections", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun listAudioClipSelections(sourceUri: String): List<AudioClipSelection> {
        val result = mutableListOf<AudioClipSelection>()
        readableDatabase.rawQuery(
            "SELECT clip_id, source_uri, start_ms, end_ms, project_id, riwayah, surah_no, ayah_no, created_at, updated_at FROM audio_clip_selections WHERE source_uri=? ORDER BY created_at DESC",
            arrayOf(sourceUri)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += AudioClipSelection(
                    id = cursor.getString(0),
                    sourceUri = cursor.getString(1),
                    startMs = cursor.getLong(2),
                    endMs = cursor.getLong(3),
                    projectId = if (cursor.isNull(4)) null else cursor.getString(4),
                    riwayah = if (cursor.isNull(5)) null else QuranRiwayah.valueOf(cursor.getString(5)),
                    surahNumber = if (cursor.isNull(6)) null else cursor.getInt(6),
                    ayahNumber = if (cursor.isNull(7)) null else cursor.getInt(7),
                    createdAt = cursor.getLong(8),
                    updatedAt = cursor.getLong(9)
                )
            }
        }
        return result
    }

    fun deleteAudioClipSelection(clipId: String) {
        writableDatabase.delete("audio_clip_selections", "clip_id=?", arrayOf(clipId))
    }

    private fun createStudioScenesTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS studio_scenes (
                scene_id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                title TEXT NOT NULL,
                scene_order INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                transition_in TEXT NOT NULL DEFAULT 'CUT',
                transition_duration_ms INTEGER NOT NULL DEFAULT 0,
                notes TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_studio_scenes_project_order ON studio_scenes(project_id, scene_order)")
    }

    fun saveStudioScene(scene: StudioScene) {
        val values = ContentValues().apply {
            put("scene_id", scene.id)
            put("project_id", scene.projectId)
            put("title", scene.title)
            put("scene_order", scene.order)
            put("duration_ms", scene.durationMs)
            put("transition_in", scene.transitionIn.name)
            put("transition_duration_ms", scene.transitionDurationMs)
            put("notes", scene.notes)
            put("created_at", scene.createdAt)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "studio_scenes", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun listStudioScenes(projectId: String): List<StudioScene> {
        val result = mutableListOf<StudioScene>()
        readableDatabase.rawQuery(
            "SELECT scene_id,project_id,title,scene_order,duration_ms,transition_in,transition_duration_ms,notes,created_at,updated_at FROM studio_scenes WHERE project_id=? ORDER BY scene_order,created_at",
            arrayOf(projectId)
        ).use { c ->
            while (c.moveToNext()) {
                result += StudioScene(
                    id = c.getString(0),
                    projectId = c.getString(1),
                    title = c.getString(2),
                    order = c.getInt(3),
                    durationMs = c.getLong(4),
                    transitionIn = SceneTransition.valueOf(c.getString(5)),
                    transitionDurationMs = c.getLong(6),
                    notes = c.getString(7),
                    createdAt = c.getLong(8),
                    updatedAt = c.getLong(9)
                )
            }
        }
        return result
    }

    fun deleteStudioScene(sceneId: String) {
        writableDatabase.delete("studio_scenes", "scene_id=?", arrayOf(sceneId))
    }

    fun replaceStudioSceneOrder(projectId: String, orderedIds: List<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            orderedIds.forEachIndexed { index, id ->
                db.execSQL(
                    "UPDATE studio_scenes SET scene_order=?, updated_at=? WHERE scene_id=? AND project_id=?",
                    arrayOf(index, System.currentTimeMillis(), id, projectId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun createSceneBackgroundTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scene_backgrounds (
                project_id TEXT NOT NULL,
                image_uri TEXT,
                video_uri TEXT,
                scene_id TEXT,
                preset TEXT NOT NULL DEFAULT 'emerald',
                motion_enabled INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(project_id, scene_id)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_scene_backgrounds_scene ON scene_backgrounds(scene_id)")
    }

    fun saveSceneBackground(background: SceneBackground) {
        val values = ContentValues().apply {
            put("project_id", background.projectId)
            put("scene_id", background.sceneId)
            put("image_uri", background.imageUri)
            put("video_uri", background.videoUri)
            put("preset", background.preset)
            put("motion_enabled", if (background.motionEnabled) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            // SQLite does not treat NULLs as equal in a composite primary key.
            // Serialize the lookup and write instead of relying on REPLACE.
            val key = if (background.sceneId == null) "project_id=? AND scene_id IS NULL"
                      else "project_id=? AND scene_id=?"
            val args = if (background.sceneId == null) arrayOf(background.projectId)
                       else arrayOf(background.projectId, background.sceneId)
            val current = db.rawQuery(
                "SELECT rowid,updated_at FROM scene_backgrounds WHERE $key ORDER BY updated_at DESC,rowid DESC LIMIT 1",
                args
            ).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getLong(1) else null }
            if (current == null) {
                db.insertOrThrow("scene_backgrounds", null, values)
            } else {
                // Keep older duplicate rows for a separately reviewed cleanup.
                // Clock corrections must not make the just-saved row look older.
                values.put("updated_at", maxOf(System.currentTimeMillis(), current.second))
                db.update("scene_backgrounds", values, "rowid=?", arrayOf(current.first.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getSceneBackground(projectId: String): SceneBackground? {
        readableDatabase.rawQuery(
            "SELECT project_id,image_uri,video_uri,preset,motion_enabled FROM scene_backgrounds WHERE project_id=? AND scene_id IS NULL ORDER BY updated_at DESC,rowid DESC LIMIT 1",
            arrayOf(projectId)
        ).use { c ->
            if (!c.moveToFirst()) return null
            return SceneBackground(
                projectId = c.getString(0),
                imageUri = if (c.isNull(1)) null else c.getString(1),
                videoUri = if (c.isNull(2)) null else c.getString(2),
                preset = c.getString(3),
                motionEnabled = c.getInt(4) == 1
            )
        }
    }

    fun getSceneBackgroundForScene(sceneId: String): SceneBackground? {
        readableDatabase.rawQuery(
            "SELECT project_id,image_uri,video_uri,preset,motion_enabled FROM scene_backgrounds WHERE scene_id=?",
            arrayOf(sceneId)
        ).use { c ->
            if (!c.moveToFirst()) return null
            return SceneBackground(
                projectId = c.getString(0),
                imageUri = if (c.isNull(1)) null else c.getString(1),
                videoUri = if (c.isNull(2)) null else c.getString(2),
                preset = c.getString(3),
                motionEnabled = c.getInt(4) == 1,
                sceneId = sceneId
            )
        }
    }

    private fun createSceneLayersTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scene_layers (
                layer_id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                layer_type TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                position_x REAL NOT NULL,
                position_y REAL NOT NULL,
                font_size REAL NOT NULL,
                visible INTEGER NOT NULL,
                z_order INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                scene_id TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_scene_layers_project ON scene_layers(project_id, z_order)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_scene_layers_scene ON scene_layers(scene_id, z_order)")
    }

    fun saveSceneLayer(layer: StudioSceneLayer) {
        val values = ContentValues().apply {
            put("layer_id", layer.id)
            put("project_id", layer.projectId)
            put("scene_id", layer.sceneId)
            put("layer_type", layer.type.name)
            put("title", layer.title)
            put("content", layer.content)
            put("start_ms", layer.startMs)
            put("end_ms", layer.endMs)
            put("position_x", layer.positionX)
            put("position_y", layer.positionY)
            put("font_size", layer.fontSize)
            put("visible", if (layer.visible) 1 else 0)
            put("z_order", layer.zOrder)
            put("created_at", layer.createdAt)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("scene_layers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun listSceneLayers(projectId: String): List<StudioSceneLayer> {
        val result = mutableListOf<StudioSceneLayer>()
        readableDatabase.rawQuery(
            "SELECT layer_id,project_id,layer_type,title,content,start_ms,end_ms,position_x,position_y,font_size,visible,z_order,created_at,updated_at FROM scene_layers WHERE project_id=? ORDER BY z_order, created_at",
            arrayOf(projectId)
        ).use { c ->
            while (c.moveToNext()) {
                result += StudioSceneLayer(
                    id = c.getString(0), projectId = c.getString(1),
                    type = SceneLayerType.valueOf(c.getString(2)),
                    title = c.getString(3), content = c.getString(4),
                    startMs = c.getLong(5), endMs = c.getLong(6),
                    positionX = c.getFloat(7), positionY = c.getFloat(8),
                    fontSize = c.getFloat(9), visible = c.getInt(10) == 1,
                    zOrder = c.getInt(11), createdAt = c.getLong(12),
                    updatedAt = c.getLong(13)
                )
            }
        }
        return result
    }

    fun listSceneLayersForScene(sceneId: String): List<StudioSceneLayer> {
        val result = mutableListOf<StudioSceneLayer>()
        readableDatabase.rawQuery(
            "SELECT layer_id,project_id,layer_type,title,content,start_ms,end_ms,position_x,position_y,font_size,visible,z_order,created_at,updated_at,scene_id FROM scene_layers WHERE scene_id=? ORDER BY z_order,created_at",
            arrayOf(sceneId)
        ).use { c ->
            while (c.moveToNext()) {
                result += StudioSceneLayer(
                    id = c.getString(0), projectId = c.getString(1),
                    type = SceneLayerType.valueOf(c.getString(2)),
                    title = c.getString(3), content = c.getString(4),
                    startMs = c.getLong(5), endMs = c.getLong(6),
                    positionX = c.getFloat(7), positionY = c.getFloat(8),
                    fontSize = c.getFloat(9), visible = c.getInt(10) == 1,
                    zOrder = c.getInt(11), createdAt = c.getLong(12),
                    updatedAt = c.getLong(13), sceneId = c.getString(14)
                )
            }
        }
        return result
    }

    fun deleteSceneLayer(layerId: String) {
        writableDatabase.delete("scene_layers", "layer_id=?", arrayOf(layerId))
    }

    fun getLatestAudioClipSelection(): AudioClipSelection? {
        readableDatabase.rawQuery(
            "SELECT clip_id, source_uri, start_ms, end_ms, project_id, riwayah, surah_no, ayah_no, created_at, updated_at FROM audio_clip_selections ORDER BY updated_at DESC LIMIT 1",
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return AudioClipSelection(
                id = cursor.getString(0),
                sourceUri = cursor.getString(1),
                startMs = cursor.getLong(2),
                endMs = cursor.getLong(3),
                projectId = if (cursor.isNull(4)) null else cursor.getString(4),
                riwayah = if (cursor.isNull(5)) null else QuranRiwayah.valueOf(cursor.getString(5)),
                surahNumber = if (cursor.isNull(6)) null else cursor.getInt(6),
                ayahNumber = if (cursor.isNull(7)) null else cursor.getInt(7),
                createdAt = cursor.getLong(8),
                updatedAt = cursor.getLong(9)
            )
        }
    }

    fun saveProject(project: StudioProject) {
        val values = ContentValues().apply {
            put("project_id", project.id)
            put("title", project.title)
            put("riwayah", project.riwayah.name)
            put("surah_no", project.surahNumber)
            put("theme_id", project.themeId)
            put("canvas_ratio", project.canvasRatio)
            put("audio_uri", project.audioUri)
            put("created_at", project.createdAt)
            put("updated_at", project.updatedAt)
        }
        writableDatabase.insertWithOnConflict(
            "studio_projects", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun listProjects(): List<StudioProject> {
        val projects = mutableListOf<StudioProject>()
        readableDatabase.rawQuery(
            "SELECT project_id,title,riwayah,surah_no,theme_id,canvas_ratio,audio_uri,created_at,updated_at FROM studio_projects ORDER BY updated_at DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                projects += StudioProject(
                    id = cursor.getString(0),
                    title = cursor.getString(1),
                    riwayah = QuranRiwayah.valueOf(cursor.getString(2)),
                    surahNumber = cursor.getInt(3),
                    themeId = cursor.getString(4),
                    canvasRatio = cursor.getString(5),
                    audioUri = if (cursor.isNull(6)) null else cursor.getString(6),
                    createdAt = cursor.getLong(7),
                    updatedAt = cursor.getLong(8)
                )
            }
        }
        return projects
    }

    fun deleteProject(projectId: String) {
        writableDatabase.delete("studio_projects", "project_id=?", arrayOf(projectId))
    }

    private fun createTimingTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ayah_timings (
                riwayah TEXT NOT NULL,
                surah_no INTEGER NOT NULL,
                ayah_no INTEGER NOT NULL,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                audio_uri TEXT,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(riwayah, surah_no, ayah_no)
            )
        """.trimIndent())
    }

    fun saveAyahTimings(
        riwayah: QuranRiwayah,
        surah: Int,
        timings: List<AyahTiming>,
        audioUri: String?
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            timings.forEach { timing ->
                val values = ContentValues().apply {
                    put("riwayah", riwayah.name)
                    put("surah_no", surah)
                    put("ayah_no", timing.ayahNumber)
                    put("start_ms", timing.startMs)
                    put("end_ms", timing.endMs)
                    put("audio_uri", audioUri)
                    put("updated_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict("ayah_timings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadAyahTimings(riwayah: QuranRiwayah, surah: Int): List<AyahTiming> {
        val result = mutableListOf<AyahTiming>()
        readableDatabase.rawQuery(
            "SELECT ayah_no, start_ms, end_ms FROM ayah_timings WHERE riwayah=? AND surah_no=? ORDER BY ayah_no",
            arrayOf(riwayah.name, surah.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += AyahTiming(cursor.getInt(0), cursor.getLong(1), cursor.getLong(2))
            }
        }
        return result
    }

    fun importVerifiedDataset(
        rawJson: ByteArray,
        manifest: QuranDatasetManifest
    ): QuranImportReport {
        val actualHash = sha256(rawJson)
        if (!actualHash.equals(manifest.expectedSha256, ignoreCase = true)) {
            throw QuranDatasetException("فشلت مطابقة SHA-256 للمصدر؛ أُوقف الاستيراد.")
        }
        val records = parseCanonicalJson(rawJson, manifest.riwayah)
        validateRecords(records, manifest.riwayah)

        val db = writableDatabase
        val table = tableFor(manifest.riwayah)
        db.beginTransaction()
        try {
            db.delete(table, null, null)
            records.forEach { record ->
                val values = ContentValues().apply {
                    put("surah_no", record.surahNumber)
                    put("ayah_no", record.ayahNumber)
                    put("text_uthmani", record.textUthmani)
                    record.juzNumber?.let { put("juz_no", it) }
                    record.pageNumber?.let { put("page_no", it) }
                }
                db.insertOrThrow(table, null, values)
            }
            val meta = ContentValues().apply {
                put("riwayah", manifest.riwayah.name)
                put("source_name", manifest.sourceName)
                put("source_version", manifest.sourceVersion)
                put("source_url", manifest.sourceUrl)
                put("sha256", actualHash)
                put("ayah_count", records.size)
                put("imported_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                "quran_dataset_manifest", null, meta, SQLiteDatabase.CONFLICT_REPLACE
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return QuranImportReport(manifest.riwayah, records.size, actualHash, manifest.sourceVersion)
    }

    fun getAyah(riwayah: QuranRiwayah, surah: Int, ayah: Int): QuranAyahRecord? {
        val table = tableFor(riwayah)
        readableDatabase.rawQuery(
            "SELECT surah_no, ayah_no, text_uthmani, juz_no, page_no FROM $table WHERE surah_no=? AND ayah_no=?",
            arrayOf(surah.toString(), ayah.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return QuranAyahRecord(
                riwayah, cursor.getInt(0), cursor.getInt(1), cursor.getString(2),
                if (cursor.isNull(3)) null else cursor.getInt(3),
                if (cursor.isNull(4)) null else cursor.getInt(4)
            )
        }
    }

    fun getDatasetStatus(riwayah: QuranRiwayah): String? {
        readableDatabase.rawQuery(
            """
            SELECT source_name, source_version, ayah_count
            FROM quran_dataset_manifest
            WHERE riwayah = ?
            """.trimIndent(),
            arrayOf(riwayah.name)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null

            val sourceName = cursor.getString(0)
            val sourceVersion = cursor.getString(1)
            val ayahCount = cursor.getInt(2)

            return "$sourceName · $sourceVersion · $ayahCount آية"
        }
    }

    private fun tableFor(riwayah: QuranRiwayah) = when (riwayah) {
        QuranRiwayah.HAFS -> "quran_ayah_hafs"
        QuranRiwayah.WARSH -> "quran_ayah_warsh"
    }

    private fun parseCanonicalJson(raw: ByteArray, riwayah: QuranRiwayah): List<QuranAyahRecord> {
        val root = JSONObject(raw.toString(Charsets.UTF_8))
        if (root.getString("riwayah") != riwayah.name) {
            throw QuranDatasetException("ملف البيانات لا يطابق الرواية المختارة.")
        }
        val array = root.getJSONArray("ayahs")
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            QuranAyahRecord(
                riwayah, item.getInt("surah"), item.getInt("ayah"),
                item.getString("text"),
                if (item.has("juz")) item.getInt("juz") else null,
                if (item.has("page")) item.getInt("page") else null
            )
        }
    }

    private fun validateRecords(records: List<QuranAyahRecord>, riwayah: QuranRiwayah) {
        if (records.isEmpty()) throw QuranDatasetException("ملف القرآن فارغ.")
        if (records.any { it.riwayah != riwayah }) throw QuranDatasetException("اختلاط الروايات.")
        if (records.any {
            it.surahNumber !in 1..114 || it.ayahNumber < 1 || it.textUthmani.isBlank()
        }) throw QuranDatasetException("بيانات سورة أو آية أو نص غير صالحة.")

        val ordered = records.sortedWith(compareBy<QuranAyahRecord> { it.surahNumber }.thenBy { it.ayahNumber })
        if (ordered != records) throw QuranDatasetException("السجلات ليست مرتبة حسب السورة والآية.")
        if (records.map { it.surahNumber }.toSet() != (1..114).toSet()) {
            throw QuranDatasetException("يجب أن يحتوي الملف على السور الـ114 كاملة.")
        }
        if (records.groupBy { it.surahNumber }.any { (_, ayahs) ->
            val nums = ayahs.map { it.ayahNumber }
            nums.size != nums.toSet().size || nums.sorted() != (1..nums.size).toList()
        }) throw QuranDatasetException("ترقيم الآيات مكرر أو غير متسلسل.")
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
