package com.almishkat.studio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SceneCompositionScreen() {
    val context = LocalContext.current
    val db = remember { QuranLocalDatabase(context) }
    var projects by remember { mutableStateOf(db.listProjects()) }
    var projectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var scenes by remember(projectId) { mutableStateOf(projectId?.let(db::listStudioScenes) ?: emptyList()) }
    var sceneId by remember(scenes) { mutableStateOf(scenes.firstOrNull()?.id) }
    var background by remember(sceneId) { mutableStateOf(sceneId?.let(db::getSceneBackgroundForScene)) }
    var layers by remember(sceneId) { mutableStateOf(sceneId?.let(db::listSceneLayersForScene) ?: emptyList()) }
    var projectMenu by remember { mutableStateOf(false) }
    var sceneMenu by remember { mutableStateOf(false) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var layerTitle by remember { mutableStateOf("") }
    var layerText by remember { mutableStateOf("") }

    fun saveBg(updated: SceneBackground) {
        db.saveSceneBackground(updated)
        background = updated
    }
    fun keepPermission(uri: android.net.Uri) {
        runCatching { context.contentResolver.takePersistableUriPermission(
            uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        ) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pid = projectId; val sid = sceneId
        if (uri != null && pid != null && sid != null) {
            keepPermission(uri)
            saveBg((background ?: SceneBackground(pid, sceneId = sid)).copy(
                imageUri = uri.toString(), videoUri = null, sceneId = sid
            ))
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pid = projectId; val sid = sceneId
        if (uri != null && pid != null && sid != null) {
            keepPermission(uri)
            saveBg((background ?: SceneBackground(pid, sceneId = sid)).copy(
                videoUri = uri.toString(), sceneId = sid
            ))
        }
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تركيب كل مشهد", color = AlmishkatTokens.LightIvory, fontSize = 19.sp)
        Text("لكل مشهد خلفيته وطبقاته النصية المستقلة.",
            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
        if (projects.isEmpty()) {
            Text("أنشئ مشروعاً أولاً.", color = AlmishkatTokens.TajweedAmber)
        } else {
            Box {
                OutlinedButton(onClick = { projectMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(projects.firstOrNull { it.id == projectId }?.title ?: "اختر المشروع")
                }
                DropdownMenu(projectMenu, { projectMenu = false }) {
                    projects.forEach { project ->
                        DropdownMenuItem(text = { Text(project.title) }, onClick = {
                            projectId = project.id
                            scenes = db.listStudioScenes(project.id)
                            sceneId = scenes.firstOrNull()?.id
                            projectMenu = false
                        })
                    }
                }
            }
            if (scenes.isEmpty()) {
                Text("أضف مشهداً من تبويب «المشاهد» أولاً.",
                    color = AlmishkatTokens.TajweedAmber, fontSize = 11.sp)
            } else {
                Box {
                    OutlinedButton(onClick = { sceneMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(scenes.firstOrNull { it.id == sceneId }?.title ?: "اختر المشهد")
                    }
                    DropdownMenu(sceneMenu, { sceneMenu = false }) {
                        scenes.forEach { scene ->
                            DropdownMenuItem(text = { Text("${scene.order + 1}. ${scene.title}") },
                                onClick = { sceneId = scene.id; sceneMenu = false })
                        }
                    }
                }
                Surface(color = AlmishkatTokens.SanctuarySurface, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("خلفية هذا المشهد", color = AlmishkatTokens.LightIvory)
                        Text(when {
                            background?.videoUri != null -> "فيديو محفوظ لهذا المشهد"
                            background?.imageUri != null -> "صورة محفوظة لهذا المشهد"
                            else -> "سيستخدم المشهد الخلفية الافتراضية"
                        }, color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Image, null); Text("صورة")
                            }
                            OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) },
                                modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Movie, null); Text("فيديو")
                            }
                        }
                        Text("طبقات هذا المشهد (${layers.size})", color = AlmishkatTokens.MushafGold)
                        if (layers.isNotEmpty()) {
                            LazyColumn(Modifier.heightIn(max = 180.dp)) {
                                items(layers, key = { it.id }) { layer ->
                                    Text("• ${layer.title}: ${layer.content}",
                                        color = AlmishkatTokens.LightIvory, fontSize = 11.sp)
                                }
                            }
                        }
                        Button(onClick = { showLayerDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Add, null); Text("إضافة نص لهذا المشهد")
                        }
                    }
                }
            }
        }
    }
    if (showLayerDialog) {
        AlertDialog(
            onDismissRequest = { showLayerDialog = false },
            title = { Text("إضافة طبقة للمشهد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(layerTitle, { layerTitle = it },
                        label = { Text("اسم الطبقة") }, singleLine = true)
                    OutlinedTextField(layerText, { layerText = it },
                        label = { Text("النص") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pid = projectId; val sid = sceneId
                    if (pid != null && sid != null && layerText.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        db.saveSceneLayer(StudioSceneLayer(
                            projectId = pid, sceneId = sid, type = SceneLayerType.TITLE,
                            title = layerTitle.ifBlank { "نص المشهد" }, content = layerText.trim(),
                            startMs = 0L,
                            endMs = scenes.firstOrNull { it.id == sid }?.durationMs ?: 8_000L,
                            zOrder = layers.size, createdAt = now, updatedAt = now
                        ))
                        layers = db.listSceneLayersForScene(sid)
                    }
                    layerTitle = ""; layerText = ""; showLayerDialog = false
                }, enabled = sceneId != null && layerText.isNotBlank()) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showLayerDialog = false }) { Text("إلغاء") } }
        )
    }
}
