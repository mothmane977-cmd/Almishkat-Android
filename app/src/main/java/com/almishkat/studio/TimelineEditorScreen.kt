package com.almishkat.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val timelineColors = listOf(
    AlmishkatTokens.RoyalEmerald,
    AlmishkatTokens.MushafGold,
    AlmishkatTokens.TajweedCyan,
    AlmishkatTokens.TajweedIndigo,
    AlmishkatTokens.TajweedSage
)

@Composable
fun TimelineEditorScreen() {
    val projectDuration = 60_000L
    var playhead by remember { mutableLongStateOf(0L) }
    var clips by remember {
        mutableStateOf(
            listOf(
                TimelineClip(track = TimelineTrackType.BACKGROUND, title = "خلفية المشهد",
                    startMs = 0, endMs = projectDuration),
                TimelineClip(track = TimelineTrackType.QURAN_TEXT, title = "طبقة الآيات",
                    startMs = 0, endMs = 18_000),
                TimelineClip(track = TimelineTrackType.RECITATION_AUDIO, title = "التلاوة",
                    startMs = 0, endMs = 42_000),
                TimelineClip(track = TimelineTrackType.CAPTION, title = "الترجمة",
                    startMs = 3_000, endMs = 18_000)
            )
        )
    }
    var selectedTrack by remember { mutableStateOf(TimelineTrackType.RECITATION_AUDIO) }
    val errors = remember(clips) { TimelineRules.validate(clips, projectDuration) }

    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("الخط الزمني متعدد المسارات", color = AlmishkatTokens.LightIvory,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("بنية تحرير أولية لمشهد عمودي 9:16",
                    color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
            }
            Text("${QuranSyncEngine.formatTime(playhead)} / 01:00",
                color = AlmishkatTokens.MushafGold, fontSize = 11.sp)
        }

        Surface(color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlmishkatTokens.GlassBorder)) {
            Column(Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("مؤشر التشغيل", color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                Slider(
                    value = playhead.toFloat(),
                    onValueChange = { playhead = TimelineRules.clampPlayhead(it.toLong(), projectDuration) },
                    valueRange = 0f..projectDuration.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = AlmishkatTokens.MushafGold,
                        activeTrackColor = AlmishkatTokens.MushafGold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0L, 10_000L, 20_000L, 30_000L, 40_000L, 50_000L, 60_000L).forEach { t ->
                        Text(QuranSyncEngine.formatTime(t), color = AlmishkatTokens.TextSecondary,
                            fontSize = 8.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Text("المسارات", color = AlmishkatTokens.LightIvory,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
        TimelineTrackType.values().forEachIndexed { index, track ->
            val trackClips = clips.filter { it.track == track }
            val selected = selectedTrack == track
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(AlmishkatTokens.SanctuarySurface)
                    .border(1.dp, if (selected) AlmishkatTokens.MushafGold.copy(alpha=.65f)
                        else AlmishkatTokens.GlassBorder, RoundedCornerShape(15.dp))
                    .clickable { selectedTrack = track }.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(track.arabicLabel, color = if (selected) AlmishkatTokens.MushafGold
                        else AlmishkatTokens.LightIvory, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${trackClips.size} مقطع", color = AlmishkatTokens.TextSecondary, fontSize = 9.sp)
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)
                    .clip(RoundedCornerShape(9.dp)).background(AlmishkatTokens.MihrabNight)) {
                    val widthPx = constraints.maxWidth.toFloat()
                    Canvas(Modifier.fillMaxSize()) {
                        drawLine(AlmishkatTokens.TextSecondary.copy(alpha=.22f),
                            Offset(0f, size.height/2), Offset(size.width, size.height/2), 1f)
                        for (tick in 0..6) {
                            val x = size.width * tick / 6f
                            drawLine(AlmishkatTokens.TextSecondary.copy(alpha=.22f),
                                Offset(x, 0f), Offset(x, size.height), 1f)
                        }
                    }
                    trackClips.forEach { clip ->
                        val start = (clip.startMs.toFloat()/projectDuration).coerceIn(0f,1f)
                        val end = (clip.endMs.toFloat()/projectDuration).coerceIn(0f,1f)
                        Box(
                            Modifier.offset(x = (maxWidth * start), y = 4.dp)
                                .width((maxWidth * (end-start)).coerceAtLeast(18.dp))
                                .height(28.dp).clip(RoundedCornerShape(7.dp))
                                .background(timelineColors[index].copy(alpha=.78f))
                                .padding(horizontal=6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(clip.title, color = AlmishkatTokens.MihrabNight,
                                fontSize = 8.sp, maxLines = 1)
                        }
                    }
                    val headX = maxWidth * (playhead.toFloat()/projectDuration)
                    Box(Modifier.offset(x = headX - 1.dp).width(2.dp).height(36.dp)
                        .background(AlmishkatTokens.LightIvory))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                val next = TimelineClip(
                    track = selectedTrack,
                    title = when(selectedTrack) {
                        TimelineTrackType.BACKGROUND -> "خلفية جديدة"
                        TimelineTrackType.QURAN_TEXT -> "طبقة آية"
                        TimelineTrackType.RECITATION_AUDIO -> "مقطع تلاوة"
                        TimelineTrackType.CAPTION -> "ترجمة"
                        TimelineTrackType.EFFECT -> "مؤثر"
                    },
                    startMs = playhead,
                    endMs = (playhead + 10_000L).coerceAtMost(projectDuration)
                )
                if (next.endMs > next.startMs) clips = clips + next
            }, enabled = playhead < projectDuration) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(5.dp))
                Text("إضافة مقطع")
            }
            OutlinedButton(onClick = {
                val last = clips.lastOrNull { it.track == selectedTrack }
                if (last != null) clips = clips - last
            }, enabled = clips.any { it.track == selectedTrack }) {
                Text("حذف آخر مقطع")
            }
        }

        Surface(color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("فحص بنية المسارات", color = AlmishkatTokens.MushafGold,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (errors.isEmpty()) Text("لا توجد أخطاء بنيوية في المقاطع الحالية.",
                    color = AlmishkatTokens.TajweedSage, fontSize = 10.sp)
                else errors.forEach { Text("• $it", color = AlmishkatTokens.TajweedAmber, fontSize = 10.sp) }
                Text("هذه معاينة هيكلية للمسارات وليست موجة صوتية مستخرجة من الملف. لم يُنفّذ بعد تحريك المقاطع بالسحب أو التصدير.",
                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}
