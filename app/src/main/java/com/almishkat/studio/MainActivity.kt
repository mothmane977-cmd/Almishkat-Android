package com.almishkat.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val T = AlmishkatTokens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AlmishkatStudioApp() }
    }
}

@Composable
private fun AlmishkatStudioApp() {
    val scheme = darkColorScheme(
        primary = T.MushafGold, onPrimary = T.MihrabNight,
        background = T.MihrabNight, onBackground = T.LightIvory,
        surface = T.SanctuarySurface, onSurface = T.LightIvory,
        secondary = T.RoyalEmerald, onSecondary = T.LightIvory
    )
    MaterialTheme(colorScheme = scheme) {
        var selectedProject by remember { mutableStateOf<StudioProject?>(null) }
        if (selectedProject == null) {
            StudioProjectsScreen(onOpenProject = { selectedProject = it })
        } else {
            StudioEditorScreen(
                project = selectedProject!!,
                onBack = { selectedProject = null },
                onOpenProject = { selectedProject = it }
            )
        }
    }
}

@Composable
private fun StudioEditorScreen(
    project: StudioProject,
    onBack: () -> Unit,
    onOpenProject: (StudioProject) -> Unit
) {
    var activeTab by remember { mutableStateOf("المحتوى") }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf("محراب الليل") }
    var textSize by remember { mutableFloatStateOf(30f) }
    var dimLevel by remember { mutableFloatStateOf(.42f) }
    var showVerse by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = T.MihrabNight,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape)
                        .background(T.SanctuarySurface).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = T.MushafGold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ALMISHKAT", color = T.MushafGold, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("استوديو التحرير", color = T.LightIvory, fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold)
                }
                Text("حفظ تلقائي", color = T.TextSecondary, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(7.dp).clip(CircleShape).background(T.TajweedSage))
            }
        },
        bottomBar = { EditorBottomBar(activeTab) { activeTab = it } }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.title, color = T.LightIvory, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                    Text("${project.riwayah.arabicName} • سورة ${project.surahNumber} • مشهد ${project.canvasRatio}",
                        color = T.TextSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { activeTab = "تصدير الفيديو" },
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = T.MushafGold,
                        contentColor = T.MihrabNight
                    )
                ) {
                    Text("تصدير MP4", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Main portrait composition stage.
            Box(
                Modifier.fillMaxWidth().aspectRatio(.78f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(T.RoyalEmerald, T.MihrabNight, T.SanctuarySurface)
                        )
                    )
                    .border(1.dp, T.GlassBorder, RoundedCornerShape(26.dp))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Subtle radial-like horizon glow and mosque-inspired silhouette.
                    drawCircle(
                        Brush.radialGradient(
                            listOf(T.MushafGold.copy(alpha = .16f), T.MushafGold.copy(alpha = 0f)),
                            center = Offset(w * .52f, h * .64f), radius = w * .62f
                        ),
                        radius = w * .62f, center = Offset(w * .52f, h * .64f)
                    )
                    val silhouette = Path().apply {
                        moveTo(0f, h * .84f)
                        lineTo(w * .18f, h * .84f)
                        quadraticTo(w * .25f, h * .71f, w * .33f, h * .84f)
                        lineTo(w * .39f, h * .84f)
                        lineTo(w * .39f, h * .31f)
                        lineTo(w * .42f, h * .31f)
                        lineTo(w * .42f, h * .22f)
                        quadraticTo(w * .50f, h * .15f, w * .58f, h * .22f)
                        lineTo(w * .58f, h * .31f)
                        lineTo(w * .61f, h * .31f)
                        lineTo(w * .61f, h * .84f)
                        quadraticTo(w * .72f, h * .69f, w * .81f, h * .84f)
                        lineTo(w, h * .84f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(silhouette, T.MihrabNight.copy(alpha = .50f))
                    // Minaret tip and delicate gold crescent-like finial.
                    drawLine(T.MushafGold.copy(alpha = .55f),
                        Offset(w * .505f, h * .16f), Offset(w * .505f, h * .08f),
                        strokeWidth = 2f)
                    drawCircle(T.MushafGold.copy(alpha = .75f), 3.5f,
                        Offset(w * .505f, h * .075f))
                }

                Column(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("ALMISHKAT", color = T.MushafGold, fontSize = 9.sp,
                            letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(T.MihrabNight.copy(alpha = .58f))
                                .border(1.dp, T.GlassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("الفَاتِحَة", color = T.MushafGold, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (showVerse) {
                        Text(
                            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            color = T.LightIvory,
                            fontSize = textSize.sp,
                            lineHeight = (textSize * 1.75f).sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (showTranslation) {
                        Spacer(Modifier.height(10.dp))
                        Text("In the name of Allah, the Most Merciful",
                            color = T.LightIvory.copy(alpha = .82f),
                            fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(T.MihrabNight.copy(alpha = .70f))
                            .border(1.dp, T.GlassBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = T.MushafGold,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("أبو بكر الشاطري", color = T.LightIvory, fontSize = 10.sp)
                            Text("تلاوة تجريبية • 00:06", color = T.TextSecondary, fontSize = 8.sp)
                        }
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(T.MushafGold).clickable { isPlaying = !isPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null, tint = T.MihrabNight
                            )
                        }
                    }
                }
            }

            // Quick action row, thumb-reachable.
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickAction(Modifier.weight(1f), Icons.Rounded.Add, "إضافة آية") { activeTab = "المحتوى" }
                QuickAction(Modifier.weight(1f), Icons.Rounded.MusicNote, "اختيار تلاوة") { activeTab = "التزامن" }
                QuickAction(Modifier.weight(1f), Icons.Rounded.Image, "الخلفية") { activeTab = "الخلفية" }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (activeTab) {
                        "الخلفية" -> "هوية المشهد"
                        "الخط" -> "تنسيق النص القرآني"
                        "التزامن" -> "الصوت والتزامن"
                        else -> "محتوى المشهد"
                    },
                    color = T.LightIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Tune, null, tint = T.MushafGold, modifier = Modifier.size(18.dp))
            }

            when (activeTab) {
                "الخلفية" -> {
                    SceneBackgroundScreen()
                }
                "المشاهد" -> {
                    StudioScenesScreen()
                }
                "تركيب المشاهد" -> {
                    SceneCompositionScreen()
                }
                "طبقات المشهد" -> {
                    SceneLayersScreen()
                }
                "تصدير الفيديو" -> {
                    VideoExportScreen()
                }
                "معاينة الفيديو" -> {
                    VideoPreviewScreen()
                }
                "الموجة الصوتية" -> {
                    AudioWaveformScreen()
                }
                "الخط الزمني" -> {
                    TimelineEditorScreen()
                }
                "مشاريعي" -> {
                    StudioProjectsScreen(onOpenProject = onOpenProject)
                }
                "المصحف" -> {
                    QuranReaderScreen()
                }
                "التزامن" -> {
                    QuranSyncScreen()
                }
                "الخلفية" -> {
                    Text("الثيمات البصرية", color = T.TextSecondary, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("محراب الليل", "روضة الهدى", "رقوق الأندلس",
                            "فجر اليقين", "المصحف التفاعلي").forEach { theme ->
                            ThemeRow(theme, selectedTheme == theme) { selectedTheme = theme }
                        }
                    }
                    Text("تعتيم الخلفية", color = T.TextSecondary, fontSize = 12.sp)
                    Slider(value = dimLevel, onValueChange = { dimLevel = it },
                        colors = SliderDefaults.colors(thumbColor = T.MushafGold,
                            activeTrackColor = T.MushafGold, inactiveTrackColor = T.RoyalEmerald))
                }
                "الخط" -> {
                    SettingSwitch("إظهار الآية", showVerse) { showVerse = it }
                    SettingSwitch("إظهار الترجمة", showTranslation) { showTranslation = it }
                    Text("حجم الخط  •  ${textSize.toInt()}", color = T.TextSecondary, fontSize = 12.sp)
                    Slider(value = textSize, onValueChange = { textSize = it },
                        valueRange = 22f..42f,
                        colors = SliderDefaults.colors(thumbColor = T.MushafGold,
                            activeTrackColor = T.MushafGold, inactiveTrackColor = T.RoyalEmerald))
                    Text("خط المصحف: يُربط بخط قرآني موثّق حسب الرواية قبل التصدير.",
                        color = T.TextSecondary, fontSize = 11.sp)
                }
                else -> {
                    SettingRow(Icons.Rounded.TextFields, "السورة", "الفاتحة")
                    SettingRow(Icons.Rounded.AutoAwesome, "الآية", "البسملة — نص تجريبي للمعاينة")
                    Text("تنبيه: النص الظاهر في هذه المعاينة عنصر تصميم تجريبي، وليس بديلاً عن مصدر قرآني موثّق.",
                        color = T.TajweedAmber, fontSize = 11.sp, lineHeight = 17.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun EditorBottomBar(active: String, onSelect: (String) -> Unit) {
    val tabs = listOf("المحتوى", "مشاريعي", "المصحف", "التزامن", "الموجة الصوتية", "الخط الزمني", "المشاهد", "تركيب المشاهد", "طبقات المشهد", "معاينة الفيديو", "تصدير الفيديو", "الخلفية", "الخط")
    val icons = listOf(Icons.Rounded.TextFields, Icons.Rounded.FolderOpen, Icons.Rounded.Book,
        Icons.Rounded.GraphicEq, Icons.Rounded.AudioFile, Icons.Rounded.GraphicEq,
        Icons.Rounded.Movie, Icons.Rounded.Image, Icons.Rounded.Layers, Icons.Rounded.PlayArrow, Icons.Rounded.Movie,
        Icons.Rounded.Image, Icons.Rounded.Tune)
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(24.dp))
            .background(T.SanctuarySurface.copy(alpha = .96f))
            .border(1.dp, T.GlassBorder, RoundedCornerShape(24.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = active == tab
            Column(
                Modifier.width(76.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (selected) T.RoyalEmerald.copy(alpha = .42f) else T.SanctuarySurface.copy(alpha = 0f))
                    .clickable { onSelect(tab) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(icons[index], null,
                    tint = if (selected) T.MushafGold else T.TextSecondary,
                    modifier = Modifier.size(20.dp))
                Text(tab, color = if (selected) T.MushafGold else T.TextSecondary,
                    fontSize = 9.sp, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(T.SanctuarySurface)
            .border(1.dp, T.GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = T.MushafGold, modifier = Modifier.size(20.dp))
        Text(label, color = T.LightIvory, fontSize = 10.sp)
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(T.SanctuarySurface).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = T.MushafGold)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = T.LightIvory, fontSize = 13.sp)
            Text(value, color = T.TextSecondary, fontSize = 10.sp)
        }
        Icon(Icons.Rounded.ExpandMore, null, tint = T.TextSecondary)
    }
}

@Composable
private fun ThemeRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) T.RoyalEmerald.copy(alpha = .32f) else T.SanctuarySurface)
            .border(1.dp, if (selected) T.MushafGold.copy(alpha = .75f) else T.GlassBorder,
                RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape)
            .background(if (selected) T.MushafGold else T.TextSecondary.copy(alpha = .45f)))
        Spacer(Modifier.width(12.dp))
        Text(title, color = if (selected) T.MushafGold else T.LightIvory,
            fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, null, tint = T.MushafGold,
            modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(T.SanctuarySurface).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = T.LightIvory, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = T.MihrabNight,
                checkedTrackColor = T.MushafGold, uncheckedThumbColor = T.TextSecondary,
                uncheckedTrackColor = T.RoyalEmerald))
    }
}
