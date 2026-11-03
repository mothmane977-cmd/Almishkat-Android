package com.almishkat.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt

@Composable
fun SceneLayersScreen() {
    val context = LocalContext.current
    val db = remember { QuranLocalDatabase(context) }
    var projects by remember { mutableStateOf(db.listProjects()) }
    var projectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var layers by remember(projectId) {
        mutableStateOf(projectId?.let(db::listSceneLayers) ?: emptyList())
    }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var layerType by remember { mutableStateOf(SceneLayerType.TITLE) }
    var layerTitle by remember { mutableStateOf("") }
    var layerContent by remember { mutableStateOf("") }
    val selectedLayer = layers.firstOrNull { it.id == selectedLayerId }

    fun persist(updated: StudioSceneLayer) {
        db.saveSceneLayer(updated)
        layers = db.listSceneLayers(updated.projectId)
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Layers, null, tint = AlmishkatTokens.MushafGold)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("محرر طبقات المشهد", color = AlmishkatTokens.LightIvory,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("اسحب العنصر داخل اللوحة، ثم اضبط حجمه ومدة ظهوره",
                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
            }
            IconButton(onClick = { projects = db.listProjects(); showCreate = true }) {
                Icon(Icons.Rounded.Add, null, tint = AlmishkatTokens.MushafGold)
            }
        }

        if (projects.isEmpty()) {
            Text("أنشئ مشروعاً من تبويب «مشاريعي» أولاً حتى تحفظ الطبقات داخله.",
                color = AlmishkatTokens.TajweedAmber, fontSize = 11.sp)
        } else {
            var menu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(projects.firstOrNull { it.id == projectId }?.title ?: "اختر المشروع")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    projects.forEach { project ->
                        DropdownMenuItem(text = { Text(project.title) }, onClick = {
                            projectId = project.id
                            selectedLayerId = null
                            menu = false
                        })
                    }
                }
            }

            // Live 9:16 layout canvas; drag a selected text layer to reposition it.
            BoxWithConstraints(
                Modifier.fillMaxWidth().aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AlmishkatTokens.RoyalEmerald)
                    .border(1.dp, AlmishkatTokens.MushafGold.copy(alpha = .55f),
                        RoundedCornerShape(22.dp))
            ) {
                val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                    Text("ALMISHKAT  •  المشكاة",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
                        color = AlmishkatTokens.MushafGold, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
                    layers.filter { it.visible }.forEach { layer ->
                        val isSelected = layer.id == selectedLayerId
                        Text(
                            text = layer.content,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (layer.positionX.coerceIn(0f, 1f) * canvasWidth).roundToInt(),
                                        (layer.positionY.coerceIn(0f, 1f) * canvasHeight).roundToInt()
                                    )
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AlmishkatTokens.MushafGold.copy(alpha = .18f)
                                    else Color.Transparent)
                                .border(if (isSelected) 1.dp else 0.dp,
                                    if (isSelected) AlmishkatTokens.MushafGold else Color.Transparent,
                                    RoundedCornerShape(8.dp))
                                .pointerInput(layer.id, canvasWidth, canvasHeight) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val current = layers.firstOrNull { it.id == layer.id }
                                            if (current != null) persist(current)
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val current = layers.firstOrNull { it.id == layer.id } ?: return@detectDragGestures
                                        selectedLayerId = layer.id
                                        val next = current.copy(
                                            positionX = (current.positionX + dragAmount.x / canvasWidth).coerceIn(0f, 1f),
                                            positionY = (current.positionY + dragAmount.y / canvasHeight).coerceIn(0f, 1f)
                                        )
                                        layers = layers.map { if (it.id == layer.id) next else it }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            color = AlmishkatTokens.LightIvory,
                            fontSize = layer.fontSize.coerceIn(10f, 64f).sp,
                            fontWeight = if (layer.type == SceneLayerType.QURAN) FontWeight.Medium else FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Text("${layers.size} طبقة • اضغط لاختيارها واسحبها لتغيير موضعها",
                color = AlmishkatTokens.MushafGold, fontSize = 11.sp)

            if (selectedLayer != null) {
                Surface(color = AlmishkatTokens.SanctuarySurface,
                    shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("تحرير: ${selectedLayer.title}", color = AlmishkatTokens.LightIvory,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("حجم الخط: ${selectedLayer.fontSize.toInt()}",
                            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                        Slider(
                            value = selectedLayer.fontSize.coerceIn(10f, 64f),
                            onValueChange = { value ->
                                layers = layers.map {
                                    if (it.id == selectedLayer.id) it.copy(fontSize = value) else it
                                }
                            },
                            onValueChangeFinished = {
                                layers.firstOrNull { it.id == selectedLayer.id }?.let(::persist)
                            },
                            valueRange = 10f..64f
                        )
                        Text("بداية الظهور: ${QuranSyncEngine.formatTime(selectedLayer.startMs)}",
                            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                        var startText by remember(selectedLayer.id, selectedLayer.startMs) {
                            mutableStateOf(selectedLayer.startMs.toString())
                        }
                        var endText by remember(selectedLayer.id, selectedLayer.endMs) {
                            mutableStateOf(selectedLayer.endMs.toString())
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(startText, { startText = it.filter(Char::isDigit) },
                                modifier = Modifier.weight(1f), label = { Text("بداية ms") },
                                singleLine = true)
                            OutlinedTextField(endText, { endText = it.filter(Char::isDigit) },
                                modifier = Modifier.weight(1f), label = { Text("نهاية ms") },
                                singleLine = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val start = startText.toLongOrNull() ?: selectedLayer.startMs
                                val end = endText.toLongOrNull() ?: selectedLayer.endMs
                                if (end > start) persist(selectedLayer.copy(startMs = start, endMs = end))
                            }, modifier = Modifier.weight(1f)) { Text("حفظ التوقيت") }
                            OutlinedButton(onClick = {
                                selectedLayerId = null
                            }, modifier = Modifier.weight(1f)) { Text("إلغاء التحديد") }
                        }
                    }
                }
            }

            if (layers.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(layers, key = { it.id }) { layer ->
                        Surface(
                            color = if (layer.id == selectedLayerId)
                                AlmishkatTokens.RoyalEmerald else AlmishkatTokens.SanctuarySurface,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(layer.title, color = AlmishkatTokens.LightIvory,
                                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(layer.type.label, color = AlmishkatTokens.TextSecondary,
                                        fontSize = 9.sp)
                                }
                                TextButton(onClick = { selectedLayerId = layer.id }) {
                                    Text("تحرير")
                                }
                                Switch(checked = layer.visible, onCheckedChange = {
                                    persist(layer.copy(visible = it))
                                })
                                IconButton(onClick = {
                                    db.deleteSceneLayer(layer.id)
                                    layers = db.listSceneLayers(layer.projectId)
                                    if (selectedLayerId == layer.id) selectedLayerId = null
                                }) {
                                    Icon(Icons.Rounded.DeleteOutline, null,
                                        tint = AlmishkatTokens.TajweedAmber)
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("إضافة طبقة")
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("طبقة جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SceneLayerType.values().forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = layerType == type, onClick = { layerType = type })
                            Text(type.label, fontSize = 12.sp)
                        }
                    }
                    OutlinedTextField(value = layerTitle, onValueChange = { layerTitle = it },
                        label = { Text("اسم الطبقة") }, singleLine = true)
                    OutlinedTextField(value = layerContent, onValueChange = { layerContent = it },
                        label = { Text("النص أو المحتوى") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = projectId
                    if (id != null) {
                        val now = System.currentTimeMillis()
                        val defaultPositionY = when (layerType) {
                            SceneLayerType.QURAN -> .48f
                            SceneLayerType.TITLE -> .22f
                            SceneLayerType.CAPTION -> .78f
                            SceneLayerType.DECORATION -> .5f
                        }
                        db.saveSceneLayer(StudioSceneLayer(
                            projectId = id, type = layerType,
                            title = layerTitle.trim().ifBlank { layerType.label },
                            content = layerContent.trim(),
                            positionX = .5f, positionY = defaultPositionY,
                            fontSize = if (layerType == SceneLayerType.QURAN) 28f else 18f,
                            zOrder = layers.size, createdAt = now, updatedAt = now
                        ))
                        layers = db.listSceneLayers(id)
                    }
                    layerTitle = ""
                    layerContent = ""
                    showCreate = false
                }, enabled = projectId != null && layerContent.isNotBlank()) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("إلغاء") }
            }
        )
    }
}
