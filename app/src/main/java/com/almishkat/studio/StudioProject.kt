package com.almishkat.studio

import java.util.UUID

data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val riwayah: QuranRiwayah = QuranRiwayah.HAFS,
    val surahNumber: Int = 1,
    val themeId: String = "mihrab-night",
    val canvasRatio: String = "9:16",
    val audioUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
