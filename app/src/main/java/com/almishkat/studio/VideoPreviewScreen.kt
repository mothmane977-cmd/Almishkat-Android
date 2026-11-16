package com.almishkat.studio

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun VideoPreviewScreen() {
    val context = LocalContext.current
    val db = remember { QuranLocalDatabase(context) }
    var clip by remember { mutableStateOf(db.getLatestAudioClipSelection()) }
    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val riwayah = clip?.riwayah ?: QuranRiwayah.HAFS
    val ayahRecord = remember(clip?.riwayah, clip?.surahNumber, clip?.ayahNumber, refreshKey) {
        val selected = clip
        if (selected?.surahNumber != null && selected.ayahNumber != null) {
            db.getAyah(riwayah, selected.surahNumber, selected.ayahNumber)
        } else null
    }

    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.stop() }
            player.release()
        }
    }

    LaunchedEffect(playing, clip?.id) {
        if (playing && clip != null) {
            try {
                player.reset()
                player.setDataSource(context, Uri.parse(clip!!.sourceUri))
                player.setOnPreparedListener { media ->
                    val start = clip!!.startMs.coerceIn(0L, media.duration.toLong())
                    media.seekTo(start.toInt())
                    media.start()
                }
                player.setOnCompletionListener { playing = false }
                player.prepareAsync()
                playbackError = null
            } catch (t: Throwable) {
                playbackError = t.localizedMessage ?: "تعذر تشغيل ملف التلاوة"
                playing = false
            }
        } else {
            runCatching { if (player.isPlaying) player.pause() }
        }
    }

    LaunchedEffect(playing, clip?.id) {
        while (playing) {
            delay(150)
            runCatching {
                if (player.isPlaying) {
                    positionMs = player.currentPosition.toLong()
                    val end = clip?.endMs ?: Long.MAX_VALUE
                    if (positionMs >= end) {
                        player.pause()
                        playing = false
                    }
                }
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("معاينة المشهد", color = AlmishkatTokens.LightIvory,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("لوحة عمودية 9:16 مع تشغيل نطاق التلاوة",
                    color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
            }
            IconButton(onClick = {
                clip = db.getLatestAudioClipSelection()
                positionMs = 0L
                playing = false
                refreshKey++
            }) {
                Icon(Icons.Rounded.Refresh, null, tint = AlmishkatTokens.MushafGold)
            }
        }

        Box(
            Modifier.fillMaxWidth().aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(
                    listOf(AlmishkatTokens.RoyalEmerald, AlmishkatTokens.MihrabNight,
                        AlmishkatTokens.SanctuarySurface)
                ))
                .border(1.dp, AlmishkatTokens.GlassBorder, RoundedCornerShape(24.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    Brush.radialGradient(
                        listOf(AlmishkatTokens.MushafGold.copy(alpha = .17f),
                            AlmishkatTokens.MushafGold.copy(alpha = 0f)),
                        center = Offset(w * .5f, h * .57f), radius = w * .7f
                    ),
                    radius = w * .7f, center = Offset(w * .5f, h * .57f)
                )
                val path = Path().apply {
                    moveTo(0f, h * .84f)
                    lineTo(w * .16f, h * .84f)
                    quadraticTo(w * .25f, h * .70f, w * .34f, h * .84f)
                    lineTo(w * .40f, h * .84f)
                    lineTo(w * .40f, h * .30f)
                    quadraticTo(w * .50f, h * .17f, w * .60f, h * .30f)
                    lineTo(w * .60f, h * .84f)
                    quadraticTo(w * .74f, h * .69f, w * .84f, h * .84f)
                    lineTo(w, h * .84f); lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(path, AlmishkatTokens.MihrabNight.copy(alpha = .56f))
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ALMISHKAT", color = AlmishkatTokens.MushafGold,
                    fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (clip?.surahNumber != null) "السورة ${clip!!.surahNumber} • الآية ${clip!!.ayahNumber}"
                    else "معاينة المشهد القرآني",
                    color = AlmishkatTokens.MushafGold, fontSize = 12.sp
                )
                Spacer(Modifier.height(18.dp))
                if (ayahRecord != null) {
                    Text(ayahRecord!!.textUthmani,
                        color = AlmishkatTokens.LightIvory, fontSize = 25.sp,
                        lineHeight = 47.sp, textAlign = TextAlign.Center)
                } else {
                    Text("لا توجد آية موثقة مرتبطة بهذا النطاق",
                        color = AlmishkatTokens.TajweedAmber, fontSize = 13.sp,
                        textAlign = TextAlign.Center)
                }
                Spacer(Modifier.weight(1f))
                Text(riwayah.arabicName, color = AlmishkatTokens.MushafGold, fontSize = 10.sp)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(AlmishkatTokens.MihrabNight.copy(alpha = .76f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("التلاوة", color = AlmishkatTokens.LightIvory,
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (clip != null) "${QuranSyncEngine.formatTime(clip!!.startMs)} – ${QuranSyncEngine.formatTime(clip!!.endMs)}"
                            else "لم يتم ربط مقطع صوتي",
                            color = AlmishkatTokens.TextSecondary, fontSize = 9.sp
                        )
                    }
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(AlmishkatTokens.MushafGold),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (clip == null) return@IconButton
                                if (playing) {
                                    playing = false
                                    runCatching { if (player.isPlaying) player.pause() }
                                } else {
                                    playing = true
                                }
                            },
                            enabled = clip != null
                        ) {
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null, tint = AlmishkatTokens.MihrabNight)
                        }
                    }
                }
            }
        }

        Surface(color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("مصدر المعاينة", color = AlmishkatTokens.MushafGold,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    clip?.let { "مقطع محفوظ • ${it.riwayah?.arabicName ?: "الرواية غير محددة"} • ${QuranSyncEngine.formatTime(it.durationMs)}" }
                        ?: "احفظ نطاقاً صوتياً من تبويب الموجة أولاً، ثم حدّث المعاينة.",
                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp
                )
                if (playbackError != null) {
                    Text(playbackError!!, color = AlmishkatTokens.TajweedAmber, fontSize = 10.sp)
                }
                Text("الموضع الصوتي: ${QuranSyncEngine.formatTime(positionMs)}",
                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
                Text("هذه معاينة داخل التطبيق وليست تصديراً لفيديو MP4. الخلفية تصميم Compose، والنص لا يظهر إلا إذا وُجدت آية في قاعدة البيانات المحلية.",
                    color = AlmishkatTokens.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}
