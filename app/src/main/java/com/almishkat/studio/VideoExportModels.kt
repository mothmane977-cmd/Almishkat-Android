package com.almishkat.studio

import android.net.Uri

data class SceneRenderPlan(
    val scene: StudioScene,
    val background: SceneBackground?,
    val layers: List<StudioSceneLayer>
)

data class VideoExportRequest(
    val audioUri: Uri,
    val audioStartMs: Long,
    val audioEndMs: Long,
    val verseText: String,
    val surahNumber: Int?,
    val ayahNumber: Int?,
    val riwayahName: String,
    val outputUri: Uri,
    val sceneLayers: List<StudioSceneLayer> = emptyList(),
    val background: SceneBackground? = null,
    val scenes: List<SceneRenderPlan> = emptyList(),
    val width: Int = 1080,
    val height: Int = 1920,
    val frameRate: Int = 30
)

sealed interface VideoExportState {
    data object Idle : VideoExportState
    data class Running(val progress: Float, val message: String) : VideoExportState
    data class Success(val outputUri: Uri) : VideoExportState
    data class Failure(val message: String) : VideoExportState
}
