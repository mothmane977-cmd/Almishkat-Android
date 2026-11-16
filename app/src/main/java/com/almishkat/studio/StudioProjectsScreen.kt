package com.almishkat.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudioProjectsScreen(onOpenProject: (StudioProject) -> Unit) {
    val context = LocalContext.current
    val db = remember { QuranLocalDatabase(context) }
    var projects by remember { mutableStateOf(db.listProjects()) }
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var selectedRiwayah by remember { mutableStateOf(QuranRiwayah.HAFS) }

    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FolderOpen, null, tint = AlmishkatTokens.MushafGold)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("مشاريعي", color = AlmishkatTokens.LightIvory,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("${projects.size} مشروع محفوظ على هذا الهاتف",
                    color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
            }
            FilledTonalIconButton(onClick = { showCreate = true }) {
                Icon(Icons.Rounded.Add, null, tint = AlmishkatTokens.MushafGold)
            }
        }

        if (projects.isEmpty()) {
            Surface(color = AlmishkatTokens.SanctuarySurface,
                shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.FolderOpen, null,
                        tint = AlmishkatTokens.MushafGold, modifier = Modifier.size(34.dp))
                    Text("لا توجد مشاريع بعد", color = AlmishkatTokens.LightIvory,
                        fontWeight = FontWeight.SemiBold)
                    Text("أنشئ مشروعاً لحفظ إعدادات الرواية والسورة والقالب.",
                        color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                    Button(onClick = { showCreate = true }) { Text("إنشاء مشروع") }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.heightIn(max = 520.dp)) {
                items(projects, key = { it.id }) { project ->
                    Surface(color = AlmishkatTokens.SanctuarySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, AlmishkatTokens.GlassBorder
                        )) {
                        Row(Modifier.fillMaxWidth().clickable { onOpenProject(project) }.padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(project.title, color = AlmishkatTokens.LightIvory,
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${project.riwayah.arabicName} • سورة ${project.surahNumber} • ${project.canvasRatio}",
                                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp
                                )
                                Text(
                                    SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
                                        .format(Date(project.updatedAt)),
                                    color = AlmishkatTokens.TextSecondary, fontSize = 9.sp
                                )
                            }
                            IconButton(onClick = {
                                db.deleteProject(project.id)
                                projects = db.listProjects()
                            }) {
                                Icon(Icons.Rounded.DeleteOutline, null,
                                    tint = AlmishkatTokens.TajweedAmber)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("مشروع جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it },
                        label = { Text("اسم المشروع") }, singleLine = true)
                    Text("الرواية", fontSize = 12.sp)
                    QuranRiwayah.values().forEach { edition ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedRiwayah == edition,
                                onClick = { selectedRiwayah = edition })
                            Text(edition.arabicName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanTitle = title.trim().ifBlank { "مشروع المشكاة" }
                    val created = StudioProject(title = cleanTitle, riwayah = selectedRiwayah)
                    db.saveProject(created)
                    projects = db.listProjects()
                    title = ""
                    showCreate = false
                    onOpenProject(created)
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("إلغاء") }
            }
        )
    }
}
