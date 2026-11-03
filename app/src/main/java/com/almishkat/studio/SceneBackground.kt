package com.almishkat.studio

data class SceneBackground(
    val projectId: String,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val preset: String = "emerald",
    val motionEnabled: Boolean = true,
    val sceneId: String? = null
)
