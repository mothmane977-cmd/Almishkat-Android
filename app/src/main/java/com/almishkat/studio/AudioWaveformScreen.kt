package com.almishkat.studio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun AudioWaveformScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var fileLabel by remember { mutableStateOf("لم يتم اختيار ملف صوت") }
    var waveform by remember { mutableStateOf<WaveformData?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectionStart by remember { mutableFloatStateOf(0f) }
    var selectionEnd by remember { mutableFloatStateOf(1f) }
    var playhead by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var clipOffsetMs by remember { mutableLongStateOf(0L) }
    val database = remember { QuranLocalDatabase(context) }
    var savedSelections by remember(audioUri) {
        mutableStateOf(audioUri?.let { database.listAudioClipSelections(it.toString()) } ?: emptyList())
    }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    val projects = remember { database.listProjects() }
    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var selectedRiwayah by remember { mutableStateOf(QuranRiwayah.HAFS) }
    var surahNumber by remember { mutableIntStateOf(1) }
    var ayahNumber by remember { mutableIntStateOf(1) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            audioUri = uri
            fileLabel = uri.lastPathSegment?.substringAfterLast('/') ?: "ملف صوت"
            waveform = null
            error = null
            selectionStart = 0f
            selectionEnd = 1f
            playhead = 0f
            clipOffsetMs = 0L
            scope.launch {
                busy = true
                try {
                    waveform = withContext(Dispatchers.IO) {
                        AudioWaveformAnalyzer.analyze(context, uri)
                    }
                } catch (t: Throwable) {
                    error = t.localizedMessage ?: "تعذر تحليل الملف الصوتي"
                } finally {
                    busy = false
                }
            }
        }
    }

    val duration = waveform?.durationMs ?: 0L
    val selectedStartMs = (selectionStart * duration).toLong()
    val selectedEndMs = (selectionEnd * duration).toLong()
    val selectedDurationMs = (selectedEndMs - selectedStartMs).coerceAtLeast(0L)
    val visibleWindow = (1f / zoom).coerceIn(.05f, 1f)
    val windowStart = (playhead - visibleWindow / 2f).coerceIn(0f, 1f - visibleWindow)
    val windowEnd = windowStart + visibleWindow

    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.GraphicEq, null, tint = AlmishkatTokens.MushafGold)
            Spacer(Modifier.width(9.dp))
            Column {
                Text("محرر الموجة الصوتية", color = AlmishkatTokens.LightIvory,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("تحديد المقاطع ونقاط القص والتكبير",
                    color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
            }
        }

        Surface(color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlmishkatTokens.GlassBorder)) {
            Column(Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AudioFile, null, tint = AlmishkatTokens.MushafGold)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("مصدر الصوت", color = AlmishkatTokens.LightIvory, fontSize = 12.sp)
                        Text(fileLabel, color = AlmishkatTokens.TextSecondary,
                            fontSize = 10.sp, maxLines = 1)
                    }
                    TextButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                        Text("اختيار ملف", color = AlmishkatTokens.MushafGold)
                    }
                }
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(),
                        color = AlmishkatTokens.MushafGold)
                    Text("يجري فك ترميز الصوت وتحليل عيناته…",
                        color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                }
                if (error != null) {
                    Text(error!!, color = AlmishkatTokens.TajweedAmber, fontSize = 11.sp)
                }
            }
        }

        Surface(color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الموجة الصوتية ونقاط التحديد", color = AlmishkatTokens.MushafGold,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (waveform == null) {
                    Box(Modifier.fillMaxWidth().height(112.dp),
                        contentAlignment = Alignment.Center) {
                        Text("اختر ملفاً صوتياً لعرض موجته",
                            color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                    }
                } else {
                    Canvas(Modifier.fillMaxWidth().height(128.dp)) {
                        val peaks = waveform!!.peaks
                        val mid = size.height / 2f
                        val first = (windowStart * peaks.size).toInt().coerceIn(0, peaks.lastIndex)
                        val last = (windowEnd * peaks.size).toInt().coerceIn(first + 1, peaks.size)
                        val visibleCount = max(1, last - first)
                        val step = size.width / visibleCount
                        for (i in first until last) {
                            val normalized = i.toFloat() / peaks.size
                            val peak = peaks[i].coerceIn(0f, 1f)
                            val h = (peak * size.height * .86f).coerceAtLeast(2f)
                            val x = (i - first) * step + step / 2f
                            val inSelection = normalized >= selectionStart && normalized <= selectionEnd
                            drawLine(
                                if (inSelection) AlmishkatTokens.MushafGold
                                else AlmishkatTokens.TextSecondary.copy(alpha = .38f),
                                Offset(x, mid - h / 2), Offset(x, mid + h / 2),
                                strokeWidth = (step * .62f).coerceAtLeast(1f),
                                cap = StrokeCap.Round
                            )
                        }
                        val startX = ((selectionStart - windowStart) / visibleWindow * size.width)
                        val endX = ((selectionEnd - windowStart) / visibleWindow * size.width)
                        if (startX in 0f..size.width) {
                            drawLine(AlmishkatTokens.TajweedCyan, Offset(startX, 0f),
                                Offset(startX, size.height), 2.dp.toPx())
                        }
                        if (endX in 0f..size.width) {
                            drawLine(AlmishkatTokens.TajweedCrimson, Offset(endX, 0f),
                                Offset(endX, size.height), 2.dp.toPx())
                        }
                        val headX = ((playhead - windowStart) / visibleWindow * size.width)
                        if (headX in 0f..size.width) {
                            drawLine(AlmishkatTokens.LightIvory, Offset(headX, 0f),
                                Offset(headX, size.height), 1.dp.toPx())
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(QuranSyncEngine.formatTime((windowStart * duration).toLong()),
                            color = AlmishkatTokens.TextSecondary, fontSize = 9.sp)
                        Text(QuranSyncEngine.formatTime((windowEnd * duration).toLong()),
                            color = AlmishkatTokens.TextSecondary, fontSize = 9.sp)
                    }
                    Text("${waveform!!.sampleRate} Hz • ${waveform!!.channelCount} قناة • ${waveform!!.peaks.size} عينة عرض",
                        color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                }
            }
        }

        if (waveform != null) {
            Surface(color = AlmishkatTokens.SanctuarySurface,
                shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("نطاق المقطع", color = AlmishkatTokens.MushafGold,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("نقطة البداية: ${QuranSyncEngine.formatTime(selectedStartMs)}",
                        color = AlmishkatTokens.LightIvory, fontSize = 11.sp)
                    Slider(
                        value = selectionStart,
                        onValueChange = { selectionStart = it.coerceAtMost(selectionEnd - .001f) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = AlmishkatTokens.TajweedCyan,
                            activeTrackColor = AlmishkatTokens.TajweedCyan)
                    )
                    Text("نقطة النهاية: ${QuranSyncEngine.formatTime(selectedEndMs)}",
                        color = AlmishkatTokens.LightIvory, fontSize = 11.sp)
                    Slider(
                        value = selectionEnd,
                        onValueChange = { selectionEnd = it.coerceAtLeast(selectionStart + .001f) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = AlmishkatTokens.TajweedCrimson,
                            activeTrackColor = AlmishkatTokens.TajweedCrimson)
                    )
                    Text("مدة التحديد: ${QuranSyncEngine.formatTime(selectedDurationMs)}",
                        color = AlmishkatTokens.TajweedSage, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                    Divider(color = AlmishkatTokens.GlassBorder)
                    Text("موضع المؤشر", color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                    Slider(value = playhead, onValueChange = { playhead = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = AlmishkatTokens.LightIvory,
                            activeTrackColor = AlmishkatTokens.LightIvory))
                    Text("التكبير: ${"%.1f".format(zoom)}×",
                        color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                    Slider(value = zoom, onValueChange = { zoom = it },
                        valueRange = 1f..12f,
                        colors = SliderDefaults.colors(thumbColor = AlmishkatTokens.MushafGold,
                            activeTrackColor = AlmishkatTokens.MushafGold))
                            Divider(color = AlmishkatTokens.GlassBorder)
                    Text("ربط المقطع بالمشروع والآية", color = AlmishkatTokens.MushafGold,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (projects.isNotEmpty()) {
                        var projectMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { projectMenu = true },
                                modifier = Modifier.fillMaxWidth()) {
                                Text(projects.firstOrNull { it.id == selectedProjectId }?.title
                                    ?: "اختر المشروع", color = AlmishkatTokens.LightIvory)
                            }
                            DropdownMenu(expanded = projectMenu,
                                onDismissRequest = { projectMenu = false }) {
                                projects.forEach { project ->
                                    DropdownMenuItem(text = { Text(project.title) }, onClick = {
                                        selectedProjectId = project.id
                                        selectedRiwayah = project.riwayah
                                        surahNumber = project.surahNumber.coerceIn(1, 114)
                                        projectMenu = false
                                    })
                                }
                            }
                        }
                    } else {
                        Text("لا توجد مشاريع محفوظة؛ يمكنك حفظ النطاق دون ربطه بمشروع.",
                            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QuranRiwayah.values().forEach { edition ->
                            FilterChip(selected = selectedRiwayah == edition,
                                onClick = { selectedRiwayah = edition },
                                label = { Text(edition.arabicName, fontSize = 10.sp) })
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = surahNumber.toString(),
                            onValueChange = { it.toIntOrNull()?.takeIf { n -> n in 1..114 }?.let { n -> surahNumber = n } },
                            label = { Text("رقم السورة") }, modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ayahNumber.toString(),
                            onValueChange = { it.toIntOrNull()?.takeIf { n -> n in 1..300 }?.let { n -> ayahNumber = n } },
                            label = { Text("رقم الآية") }, modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            selectionStart = 0f
                            selectionEnd = 1f
                        }, modifier = Modifier.weight(1f)) { Text("تحديد الكل") }
                        FilledTonalButton(onClick = {
                            clipOffsetMs = selectedStartMs
                            playhead = selectionStart
                            val uri = audioUri
                            if (uri != null && selectedEndMs > selectedStartMs) {
                                database.saveAudioClipSelection(
                                    AudioClipSelection(
                                        sourceUri = uri.toString(),
                                        startMs = selectedStartMs,
                                        endMs = selectedEndMs,
                                        projectId = selectedProjectId,
                                        riwayah = selectedRiwayah,
                                        surahNumber = surahNumber,
                                        ayahNumber = ayahNumber
                                    )
                                )
                                savedSelections = database.listAudioClipSelections(uri.toString())
                                saveMessage = "حُفظ نطاق المقطع على هذا الهاتف"
                            }
                        }, modifier = Modifier.weight(1f),
                            enabled = audioUri != null && selectedEndMs > selectedStartMs) {
                            Text("حفظ النطاق")
                        }
                    }
                    if (saveMessage != null) {
                        Text(saveMessage!!, color = AlmishkatTokens.TajweedSage, fontSize = 10.sp)
                    }
                    Text(
                        "نطاق المقطع الحالي: ${QuranSyncEngine.formatTime(clipOffsetMs)} → ${QuranSyncEngine.formatTime(selectedEndMs)}",
                        color = AlmishkatTokens.TextSecondary, fontSize = 10.sp
                    )
                    Divider(color = AlmishkatTokens.GlassBorder)
                    Text("النطاقات المحفوظة لهذا الملف (${savedSelections.size})",
                        color = AlmishkatTokens.MushafGold, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    if (savedSelections.isEmpty()) {
                        Text("لم تحفظ نطاقات لهذا التسجيل بعد",
                            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                    } else {
                        savedSelections.forEach { saved ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${QuranSyncEngine.formatTime(saved.startMs)} – ${QuranSyncEngine.formatTime(saved.endMs)}",
                                        color = AlmishkatTokens.LightIvory, fontSize = 11.sp)
                                    Text("المدة ${QuranSyncEngine.formatTime(saved.durationMs)}",
                                        color = AlmishkatTokens.TextSecondary, fontSize = 9.sp)
                                    if (saved.surahNumber != null && saved.ayahNumber != null) {
                                        Text("${saved.riwayah?.arabicName ?: ""} • سورة ${saved.surahNumber} • آية ${saved.ayahNumber}",
                                            color = AlmishkatTokens.TajweedCyan, fontSize = 9.sp)
                                    }
                                }
                                TextButton(onClick = {
                                    selectionStart = (saved.startMs.toFloat() / duration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                    selectionEnd = (saved.endMs.toFloat() / duration.coerceAtLeast(1L)).coerceIn(0f, 1f)
                                    playhead = selectionStart
                                }) { Text("استعادة") }
                                IconButton(onClick = {
                                    database.deleteAudioClipSelection(saved.id)
                                    savedSelections = database.listAudioClipSelections(saved.sourceUri)
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

        Text(
            "هذه النسخة تضيف تحديداً زمنياً غير هدّام: لا تحذف بيانات الصوت الأصلي ولا تنشئ ملفاً مقصوصاً بعد. نقاط التحديد والتكبير تعمل محلياً داخل الشاشة، وتحتاج الخطوة التالية إلى حفظها وربطها بمسارات المشروع.",
            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp, lineHeight = 17.sp
        )
    }
}
