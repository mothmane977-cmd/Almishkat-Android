package com.almishkat.studio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class BackgroundPreset(val id: String, val title: String, val colors: List<androidx.compose.ui.graphics.Color>)

@Composable
fun SceneBackgroundScreen() {
    val context = LocalContext.current
    val db = remember { QuranLocalDatabase(context) }
    val projects = remember { db.listProjects() }
    var projectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var background by remember(projectId) {
        mutableStateOf(projectId?.let(db::getSceneBackground)
            ?: projectId?.let { SceneBackground(it) })
    }
    var projectMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val presets = listOf(
        BackgroundPreset("emerald", "زمردي", listOf(AlmishkatTokens.RoyalEmerald, AlmishkatTokens.MihrabNight)),
        BackgroundPreset("night", "ليلي", listOf(AlmishkatTokens.MihrabNight, AlmishkatTokens.SanctuarySurface)),
        BackgroundPreset("gold", "ذهبي هادئ", listOf(androidx.compose.ui.graphics.Color(0xFF51401E), AlmishkatTokens.MihrabNight)),
        BackgroundPreset("sage", "ساج", listOf(androidx.compose.ui.graphics.Color(0xFF35564B), AlmishkatTokens.MihrabNight))
    )
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val id = projectId
        if (uri != null && id != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val updated = (background ?: SceneBackground(id)).copy(imageUri = uri.toString())
            db.saveSceneBackground(updated)
            background = updated
            message = "تم حفظ الخلفية لهذا المشروع"
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val id = projectId
        if (uri != null && id != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            ) }
            val updated = (background ?: SceneBackground(id)).copy(videoUri = uri.toString())
            db.saveSceneBackground(updated)
            background = updated
            message = "تم حفظ خلفية الفيديو لهذا المشروع"
        }
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("مكتبة الخلفيات", color = AlmishkatTokens.LightIvory, fontSize = 19.sp)
        Text("اختر طابعاً بصرياً أو صورة من هاتفك. الخلفيات تحفظ محلياً لكل مشروع.",
            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
        if (projects.isEmpty()) {
            Text("أنشئ مشروعاً أولاً من تبويب مشاريعي.",
                color = AlmishkatTokens.TajweedAmber, fontSize = 11.sp)
        } else {
            Box {
                OutlinedButton(onClick = { projectMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(projects.firstOrNull { it.id == projectId }?.title ?: "اختر المشروع")
                }
                DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                    projects.forEach { p ->
                        DropdownMenuItem(text = { Text(p.title) }, onClick = {
                            projectId = p.id
                            projectMenu = false
                        })
                    }
                }
            }
            Box(Modifier.fillMaxWidth().aspectRatio(9f / 16f)
                .background(
                    Brush.verticalGradient(
                        presets.firstOrNull { it.id == background?.preset }?.colors
                            ?: presets.first().colors
                    ), RoundedCornerShape(22.dp)
                )) {
                Column(Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    Text("ALMISHKAT • المشكاة", color = AlmishkatTokens.MushafGold)
                    Text(if (background?.imageUri != null) "خلفية صورة مختارة"
                        else "معاينة طابع ${presets.firstOrNull { it.id == background?.preset }?.title ?: "زمردي"}",
                        color = AlmishkatTokens.LightIvory)
                    Text("معاينة الألوان فقط؛ ستظهر الصورة في التصدير بعد تحميلها.",
                        color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                }
            }
            Text("القوالب اللونية", color = AlmishkatTokens.MushafGold, fontSize = 12.sp)
            presets.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                val id = projectId ?: return@OutlinedButton
                                val updated = (background ?: SceneBackground(id)).copy(preset = preset.id)
                                db.saveSceneBackground(updated)
                                background = updated
                                message = "تم تطبيق قالب ${preset.title}"
                            }, modifier = Modifier.weight(1f)
                        ) { Text(preset.title) }
                    }
                }
            }
            Button(onClick = { imagePicker.launch(arrayOf("image/*")) },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Image, null)
                Spacer(Modifier.width(7.dp))
                Text("اختيار صورة من الهاتف")
            }
            OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.MotionPhotosOn, null)
                Spacer(Modifier.width(7.dp))
                Text("اختيار فيديو متحرك من الهاتف")
            }
            if (background?.videoUri != null) {
                Text("تم اختيار خلفية فيديو؛ ستُستخدم أثناء التصدير بدلاً من الصورة.",
                    color = AlmishkatTokens.TajweedSage, fontSize = 10.sp)
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Rounded.MotionPhotosOn, null, tint = AlmishkatTokens.MushafGold)
                Spacer(Modifier.width(8.dp))
                Text("حركة تكبير بطيئة أثناء التصدير", modifier = Modifier.weight(1f),
                    color = AlmishkatTokens.LightIvory, fontSize = 11.sp)
                Switch(
                    checked = background?.motionEnabled ?: true,
                    onCheckedChange = { enabled ->
                        val id = projectId ?: return@Switch
                        val updated = (background ?: SceneBackground(id)).copy(motionEnabled = enabled)
                        db.saveSceneBackground(updated)
                        background = updated
                    }
                )
            }
            if (background?.imageUri != null) {
                Text("تم اختيار صورة. ستُستخدم كخلفية عند التصدير.",
                    color = AlmishkatTokens.TajweedSage, fontSize = 10.sp)
            }
            message?.let { Text(it, color = AlmishkatTokens.TajweedSage, fontSize = 10.sp) }
        }
    }
}
