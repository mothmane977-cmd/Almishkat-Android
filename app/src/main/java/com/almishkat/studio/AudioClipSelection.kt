package com.almishkat.studio

import java.util.UUID

data class AudioClipSelection(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val startMs: Long,
    val endMs: Long,
    val projectId: String? = null,
    val riwayah: QuranRiwayah? = null,
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

    fun isValid(sourceDurationMs: Long): Boolean =
        startMs >= 0L && endMs > startMs && endMs <= sourceDurationMs
}
