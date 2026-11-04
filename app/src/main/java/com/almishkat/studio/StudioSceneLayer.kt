package com.almishkat.studio

import java.util.UUID

enum class SceneLayerType(val label: String) {
    QURAN("نص قرآني"),
    TITLE("عنوان"),
    CAPTION("ترجمة أو تعليق"),
    DECORATION("زخرفة")
}

data class StudioSceneLayer(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val type: SceneLayerType,
    val title: String,
    val content: String,
    val startMs: Long = 0L,
    val endMs: Long = 60_000L,
    val positionX: Float = .5f,
    val positionY: Float = .5f,
    val fontSize: Float = 28f,
    val visible: Boolean = true,
    val zOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sceneId: String? = null
)
