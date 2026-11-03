package com.almishkat.studio

import java.util.UUID

enum class TimelineTrackType(val arabicLabel: String) {
    BACKGROUND("الخلفية"),
    QURAN_TEXT("الآيات"),
    RECITATION_AUDIO("التلاوة"),
    CAPTION("الترجمة"),
    EFFECT("المؤثرات")
}

data class TimelineClip(
    val id: String = UUID.randomUUID().toString(),
    val track: TimelineTrackType,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val sourceUri: String? = null
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

object TimelineRules {
    fun validate(clips: List<TimelineClip>, projectDurationMs: Long): List<String> {
        val errors = mutableListOf<String>()
        clips.forEach { clip ->
            if (clip.startMs < 0) errors += "بداية مقطع ${clip.title} سالبة"
            if (clip.endMs <= clip.startMs) errors += "مدة مقطع ${clip.title} غير صالحة"
            if (clip.endMs > projectDurationMs) errors += "مقطع ${clip.title} يتجاوز مدة المشروع"
        }
        return errors
    }

    fun clampPlayhead(positionMs: Long, durationMs: Long): Long =
        positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
}
