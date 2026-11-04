package com.almishkat.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext

@Composable
fun StudioScenesScreen() {
    val context = LocalContext.current
    val db = remember { QuranLocalDatabase(context) }
    var projects by remember { mutableStateOf(db.listProjects()) }
    var projectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var scenes by remember(projectId) {
        mutableStateOf(projectId?.let(db::listStudioScenes) ?: emptyList())
    }
    var showAdd by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDuration by remember { mutableStateOf("8") }
    var selectedScene by remember { mutableStateOf<String?>(null) }
    val selected = scenes.firstOrNull { it.id == selectedScene }

    fun refresh() {
        scenes = projectId?.let(db::listStudioScenes) ?: emptyList()
    }
    fun reorder(from: Int, to: Int) {
        if (to !in scenes.indices) return
        val reordered = scenes.toMutableList().apply {
            val item = removeAt(from)
            add(to, item)
        }
        projectId?.let { db.replaceStudioSceneOrder(it, reordered.map(StudioScene::id)) }
        refresh()
    }

    Column(
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Movie, null, tint = AlmishkatTokens.MushafGold)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("استوديو المشاهد", color = AlmishkatTokens.LightIvory,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("رتّب مشاهد الفيديو وحدد مدتها وانتقالها",
                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
            }
            IconButton(onClick = { projects = db.listProjects(); showAdd = true }) {
                Icon(Icons.Rounded.Add, null, tint = AlmishkatTokens.MushafGold)
            }
        }

        if (projects.isEmpty()) {
            Text("أنشئ مشروعاً أولاً من تبويب «مشاريعي».",
                color = AlmishkatTokens.TajweedAmber, fontSize = 12.sp)
        } else {
            var projectMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { projectMenu = true },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(projects.firstOrNull { it.id == projectId }?.title ?: "اختر المشروع")
                }
                DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                    projects.forEach { project ->
                        DropdownMenuItem(text = { Text(project.title) }, onClick = {
                            projectId = project.id
                            selectedScene = null
                            projectMenu = false
                        })
                    }
                }
            }

            Surface(color = AlmishkatTokens.SanctuarySurface,
                shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${scenes.size} مشهد",
                        color = AlmishkatTokens.MushafGold, fontWeight = FontWeight.Bold)
                    Text("المدة الإجمالية: ${formatSceneDuration(scenes.sumOf { it.durationMs })}",
                        color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                }
            }

            if (scenes.isEmpty()) {
                Text("لم تضف مشاهد بعد. ابدأ بمشهد افتتاحي ثم أضف مشاهد الآيات والخاتمة.",
                    color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scenes, key = { it.id }) { scene ->
                        val index = scenes.indexOfFirst { it.id == scene.id }
                        Surface(
                            color = if (scene.id == selectedScene)
                                AlmishkatTokens.RoyalEmerald else AlmishkatTokens.SanctuarySurface,
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${index + 1}. ${scene.title}",
                                        color = AlmishkatTokens.LightIvory,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${formatSceneDuration(scene.durationMs)} • ${scene.transitionIn.arabicLabel}",
                                        color = AlmishkatTokens.TajweedCyan, fontSize = 10.sp)
                                }
                                IconButton(onClick = { reorder(index, index - 1) },
                                    enabled = index > 0) {
                                    Icon(Icons.Rounded.KeyboardArrowUp, null)
                                }
                                IconButton(onClick = { reorder(index, index + 1) },
                                    enabled = index < scenes.lastIndex) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, null)
                                }
                                IconButton(onClick = {
                                    db.deleteStudioScene(scene.id)
                                    refresh()
                                    if (selectedScene == scene.id) selectedScene = null
                                }) {
                                    Icon(Icons.Rounded.DeleteOutline, null,
                                        tint = AlmishkatTokens.TajweedAmber)
                                }
                            }
                        }
                        TextButton(onClick = { selectedScene = scene.id }) {
                            Text(if (selectedScene == scene.id) "محدد للتحرير" else "تحرير إعدادات المشهد")
                        }
                    }
                }
            }

            if (selected != null) {
                Surface(color = AlmishkatTokens.SanctuarySurface,
                    shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إعدادات ${selected.title}", color = AlmishkatTokens.LightIvory,
                            fontWeight = FontWeight.Bold)
                        Text("المدة: ${formatSceneDuration(selected.durationMs)}",
                            color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                        Slider(
                            value = selected.durationMs.toFloat().coerceIn(1_000f, 120_000f),
                            onValueChange = { value ->
                                scenes = scenes.map {
                                    if (it.id == selected.id) it.copy(durationMs = value.toLong()) else it
                                }
                            },
                            onValueChangeFinished = {
                                scenes.firstOrNull { it.id == selected.id }?.let(db::saveStudioScene)
                                refresh()
                            },
                            valueRange = 1_000f..120_000f
                        )
                        var transition by remember(selected.id, selected.transitionIn) {
                            mutableStateOf(selected.transitionIn)
                        }
                        Text("الانتقال عند بداية المشهد", color = AlmishkatTokens.TextSecondary,
                            fontSize = 11.sp)
                        SceneTransition.values().forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = transition == item,
                                    onClick = { transition = item })
                                Text(item.arabicLabel, color = AlmishkatTokens.LightIvory,
                                    fontSize = 11.sp)
                            }
                        }
                        Button(onClick = {
                            db.saveStudioScene(selected.copy(
                                transitionIn = transition,
                                transitionDurationMs = if (transition == SceneTransition.CUT) 0L else 500L
                            ))
                            refresh()
                        }, modifier = Modifier.fillMaxWidth()) { Text("حفظ إعدادات المشهد") }
                    }
                }
            }

            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("إضافة مشهد")
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("مشهد جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(newTitle, { newTitle = it },
                        label = { Text("اسم المشهد") }, singleLine = true)
                    OutlinedTextField(newDuration, { newDuration = it.filter(Char::isDigit) },
                        label = { Text("المدة بالثواني") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = projectId
                    val seconds = newDuration.toLongOrNull() ?: 8L
                    if (id != null && seconds in 1L..120L) {
                        db.saveStudioScene(StudioScene(
                            projectId = id,
                            title = newTitle.trim().ifBlank { "مشهد ${scenes.size + 1}" },
                            order = scenes.size,
                            durationMs = seconds * 1000L
                        ))
                        refresh()
                    }
                    newTitle = ""
                    newDuration = "8"
                    showAdd = false
                }, enabled = projectId != null &&
                    (newDuration.toLongOrNull() ?: 8L) in 1L..120L) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("إلغاء") }
            }
        )
    }
}

private fun formatSceneDuration(ms: Long): String {
    val totalSeconds = ms / 1000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
