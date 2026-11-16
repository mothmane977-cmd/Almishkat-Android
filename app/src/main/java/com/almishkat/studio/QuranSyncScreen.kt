package com.almishkat.studio

import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Timing model: milliseconds are stored against ayah numbers.
 * Quran text is intentionally not bundled here; production text must be imported
 * from a verified Quran source with its edition/riwayah metadata.
 */
data class AyahTiming(
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long
)

object QuranSyncEngine {
    fun activeAyah(timings: List<AyahTiming>, positionMs: Long): Int? =
        timings.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs }?.ayahNumber

    fun validate(timings: List<AyahTiming>, durationMs: Long): List<String> {
        val issues = mutableListOf<String>()
        timings.forEachIndexed { index, item ->
            if (item.startMs < 0L) issues += "بداية الآية ${item.ayahNumber} سالبة"
            if (item.endMs <= item.startMs) issues += "نهاية الآية ${item.ayahNumber} يجب أن تلي بدايتها"
            if (durationMs > 0 && item.endMs > durationMs) issues += "توقيت الآية ${item.ayahNumber} يتجاوز مدة الصوت"
            if (index > 0 && item.startMs < timings[index - 1].startMs) {
                issues += "ترتيب التوقيتات غير صحيح عند الآية ${item.ayahNumber}"
            }
        }
        return issues
    }

    fun formatTime(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) / 1000L)
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }
}

@Composable
fun QuranSyncScreen() {
    val context = LocalContext.current
    var audioUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var audioName by remember { mutableStateOf("لم يتم اختيار ملف صوت") }
    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var currentSurah by remember { mutableStateOf("الفاتحة") }
    var reciter by remember { mutableStateOf("أبو بكر الشاطري") }
    var selectedRiwayah by remember { mutableStateOf(QuranRiwayah.HAFS) }
    var riwayahMenuExpanded by remember { mutableStateOf(false) }
    val quranDb = remember { QuranLocalDatabase(context) }
    var datasetStatus by remember(selectedRiwayah) {
        mutableStateOf(quranDb.getDatasetStatus(selectedRiwayah) ?: "لم تُثبت بيانات هذه الرواية محلياً بعد")
    }
    var timings by remember(selectedRiwayah) {
        mutableStateOf(
            quranDb.loadAyahTimings(selectedRiwayah, 1).ifEmpty {
                (1..7).map { n -> AyahTiming(n, (n - 1) * 5000L, n * 5000L) }
            }
        )
    }
    var selectedAyah by remember { mutableIntStateOf(1) }

    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.stop() }
            player.release()
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            audioUri = uri
            audioName = uri.lastPathSegment?.substringAfterLast('/') ?: "ملف صوت محدد"
            playerError = null
            runCatching { player.reset() }
            runCatching {
                player.setDataSource(context, uri)
                player.setOnPreparedListener { mp ->
                    durationMs = mp.duration.toLong()
                    positionMs = 0L
                    isPlaying = false
                }
                player.setOnCompletionListener { isPlaying = false; positionMs = durationMs }
                player.setOnErrorListener { _, what, extra ->
                    playerError = "تعذر تشغيل الملف الصوتي (رمز $what/$extra)"
                    isPlaying = false
                    true
                }
                player.prepareAsync()
            }.onFailure {
                playerError = "تعذر فتح الملف: ${it.localizedMessage ?: "خطأ غير معروف"}"
            }
        }
    }

    LaunchedEffect(isPlaying, audioUri) {
        while (isPlaying && audioUri != null) {
            if (runCatching { player.isPlaying }.getOrDefault(false)) {
                positionMs = player.currentPosition.toLong()
            }
            delay(200)
        }
    }

    LaunchedEffect(selectedRiwayah, timings, audioUri) {
        quranDb.saveAyahTimings(
            riwayah = selectedRiwayah,
            surah = 1,
            timings = timings,
            audioUri = audioUri?.toString()
        )
    }

    val validationIssues = remember(timings, durationMs) {
        QuranSyncEngine.validate(timings, durationMs)
    }
    val activeAyah = QuranSyncEngine.activeAyah(timings, positionMs)

    Column(
        Modifier.fillMaxWidth().padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("محرك القرآن والتزامن", color = AlmishkatTokens.LightIvory,
            fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("اختر ملف التلاوة، ثم اضبط حدود كل آية على الخط الزمني.",
            color = AlmishkatTokens.TextSecondary, fontSize = 12.sp, lineHeight = 19.sp)

        Surface(
            color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlmishkatTokens.GlassBorder)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("رواية المصحف", color = AlmishkatTokens.MushafGold,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(onClick = { riwayahMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(selectedRiwayah.arabicName,
                            color = AlmishkatTokens.LightIvory)
                    }
                    DropdownMenu(expanded = riwayahMenuExpanded,
                        onDismissRequest = { riwayahMenuExpanded = false }) {
                        QuranRiwayah.values().forEach { edition ->
                            DropdownMenuItem(
                                text = { Text(edition.arabicName) },
                                onClick = {
                                    selectedRiwayah = edition
                                    riwayahMenuExpanded = false
                                    // Timings must be stored under edition-specific keys.
                                }
                            )
                        }
                    }
                }
                Text(
                    "تُحفظ النصوص والتوقيتات منفصلة لكل رواية. تغيير الرواية لا يحوّل نصاً أو توقيتاً تلقائياً.",
                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp,
                    lineHeight = 16.sp
                )
                Text("حالة قاعدة البيانات: $datasetStatus",
                    color = AlmishkatTokens.TajweedAmber, fontSize = 10.sp)
            }
        }

        Surface(
            color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlmishkatTokens.GlassBorder)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AudioFile, null, tint = AlmishkatTokens.MushafGold)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ملف التلاوة", color = AlmishkatTokens.LightIvory,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(audioName, color = AlmishkatTokens.TextSecondary, fontSize = 10.sp,
                            maxLines = 1)
                    }
                    TextButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }) {
                        Text("اختيار", color = AlmishkatTokens.MushafGold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = AlmishkatTokens.MushafGold)
                    Spacer(Modifier.width(8.dp))
                    Text(reciter, color = AlmishkatTokens.LightIvory, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("القارئ", color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (audioUri != null && durationMs > 0) {
                                runCatching {
                                    if (player.isPlaying) {
                                        player.pause(); isPlaying = false
                                    } else {
                                        if (positionMs >= durationMs) player.seekTo(0)
                                        player.start(); isPlaying = true
                                    }
                                }.onFailure { playerError = it.localizedMessage }
                            }
                        },
                        enabled = audioUri != null && durationMs > 0
                    ) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null, tint = AlmishkatTokens.MushafGold)
                    }
                    Text(QuranSyncEngine.formatTime(positionMs),
                        color = AlmishkatTokens.LightIvory, fontSize = 11.sp)
                    Slider(
                        value = if (durationMs > 0) positionMs.toFloat().coerceIn(0f, durationMs.toFloat()) else 0f,
                        onValueChange = { value ->
                            positionMs = value.toLong()
                            if (durationMs > 0) runCatching { player.seekTo(positionMs.toInt()) }
                        },
                        valueRange = 0f..(durationMs.coerceAtLeast(1L).toFloat()),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AlmishkatTokens.MushafGold,
                            activeTrackColor = AlmishkatTokens.MushafGold
                        )
                    )
                    Text(QuranSyncEngine.formatTime(durationMs),
                        color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                }
                if (playerError != null) {
                    Text(playerError!!, color = AlmishkatTokens.TajweedCrimson, fontSize = 11.sp)
                }
            }
        }

        Surface(
            color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("نطاق العمل — ${selectedRiwayah.arabicName}", color = AlmishkatTokens.MushafGold,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("السورة", color = AlmishkatTokens.TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(currentSurah, color = AlmishkatTokens.LightIvory, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الآية النشطة", color = AlmishkatTokens.TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(activeAyah?.let { "الآية $it" } ?: "—",
                        color = AlmishkatTokens.MushafGold, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold)
                }
                Text(
                    "ملاحظة: هذه النسخة تضبط التوقيتات وأرقام الآيات فقط. لم يتم تضمين نص المصحف؛ سيُربط لاحقاً ببيانات قرآنية موثّقة قبل اعتماد العرض أو التصدير.",
                    color = AlmishkatTokens.TajweedAmber, fontSize = 10.sp, lineHeight = 17.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("خط الآيات الزمني", color = AlmishkatTokens.LightIvory,
                fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("7 آيات • الفاتحة", color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
        }

        timings.forEachIndexed { index, timing ->
            val isSelected = selectedAyah == timing.ayahNumber
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) AlmishkatTokens.RoyalEmerald.copy(alpha = .35f)
                        else AlmishkatTokens.SanctuarySurface)
                    .border(1.dp, if (isSelected) AlmishkatTokens.MushafGold.copy(alpha = .65f)
                        else AlmishkatTokens.GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { selectedAyah = timing.ayahNumber; positionMs = timing.startMs;
                        if (durationMs > 0) runCatching { player.seekTo(timing.startMs.toInt()) } }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الآية ${timing.ayahNumber}", color = AlmishkatTokens.LightIvory,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${QuranSyncEngine.formatTime(timing.startMs)} – ${QuranSyncEngine.formatTime(timing.endMs)}",
                        color = AlmishkatTokens.MushafGold, fontSize = 11.sp)
                }
                Text("بداية المقطع", color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                Slider(
                    value = timing.startMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                    onValueChange = { value ->
                        val start = value.toLong().coerceAtMost((timing.endMs - 100L).coerceAtLeast(0L))
                        timings = timings.toMutableList().also { it[index] = timing.copy(startMs = start) }
                    },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(thumbColor = AlmishkatTokens.MushafGold,
                        activeTrackColor = AlmishkatTokens.MushafGold)
                )
                Text("نهاية المقطع", color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                Slider(
                    value = timing.endMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                    onValueChange = { value ->
                        val end = value.toLong().coerceAtLeast(timing.startMs + 100L)
                        timings = timings.toMutableList().also { it[index] = timing.copy(endMs = end) }
                    },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(thumbColor = AlmishkatTokens.MushafGold,
                        activeTrackColor = AlmishkatTokens.MushafGold)
                )
            }
        }

        Surface(color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("فحص سلامة التوقيت", color = AlmishkatTokens.MushafGold,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (validationIssues.isEmpty()) {
                    Text("لا توجد أخطاء بنيوية في التوقيتات الحالية.",
                        color = AlmishkatTokens.TajweedSage, fontSize = 11.sp)
                } else {
                    validationIssues.take(4).forEach {
                        Text("• $it", color = AlmishkatTokens.TajweedAmber, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
