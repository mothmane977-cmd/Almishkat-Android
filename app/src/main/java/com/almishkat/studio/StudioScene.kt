package com.almishkat.studio

import java.util.UUID

enum class SceneTransition(val arabicLabel: String) {
    CUT("قطع مباشر"),
    FADE("تلاشي"),
    DISSOLVE("مزج تدريجي"),
    SLIDE_UP("انزلاق للأعلى")
}

data class StudioScene(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val order: Int = 0,
    val durationMs: Long = 8_000L,
    val transitionIn: SceneTransition = SceneTransition.CUT,
    val transitionDurationMs: Long = 500L,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
