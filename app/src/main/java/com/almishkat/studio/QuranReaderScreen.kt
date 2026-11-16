package com.almishkat.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val surahNames = listOf(
    "الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس",
    "هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء",
    "الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان",
    "السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى",
    "الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور",
    "النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة",
    "المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن",
    "المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير",
    "الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد",
    "الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات",
    "القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون",
    "النصر","المسد","الإخلاص","الفلق","الناس"
)

@Composable
fun QuranReaderScreen() {
    val context = LocalContext.current
    val db = remember { QuranLocalDatabase(context) }
    var riwayah by remember { mutableStateOf(QuranRiwayah.HAFS) }
    var surah by remember { mutableIntStateOf(1) }
    var ayah by remember { mutableIntStateOf(1) }
    var menu by remember { mutableStateOf(false) }
    var record by remember { mutableStateOf(db.getAyah(riwayah, surah, ayah)) }
    var status by remember { mutableStateOf(db.getDatasetStatus(riwayah)) }

    fun refresh() {
        record = db.getAyah(riwayah, surah, ayah)
        status = db.getDatasetStatus(riwayah)
    }

    // The parent editor already provides the single vertical scroll container.
    // Do not add another verticalScroll here: nested unbounded vertical scrolling
    // caused the Quran tab to crash when opened.
    Column(
        Modifier.fillMaxWidth().padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Book, null, tint = AlmishkatTokens.MushafGold)
            Spacer(Modifier.width(9.dp))
            Column {
                Text("قارئ المصحف", color = AlmishkatTokens.LightIvory,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("قراءة محلية مرتبطة بالرواية المختارة",
                    color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
            }
        }

        Surface(
            color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlmishkatTokens.GlassBorder)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الرواية", color = AlmishkatTokens.MushafGold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuranRiwayah.values().forEach { edition ->
                        FilterChip(
                            selected = riwayah == edition,
                            onClick = { riwayah = edition; refresh() },
                            label = { Text(edition.arabicName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AlmishkatTokens.RoyalEmerald,
                                selectedLabelColor = AlmishkatTokens.MushafGold
                            )
                        )
                    }
                }
                Text("السورة", color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${surah.toString().padStart(3, '0')} — ${surahNames[surah-1]}",
                            color = AlmishkatTokens.LightIvory)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        surahNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text("${index+1}. $name") },
                                onClick = {
                                    surah = index + 1
                                    ayah = 1
                                    menu = false
                                    refresh()
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("رقم الآية", color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { if (ayah > 1) { ayah--; refresh() } }) {
                        Icon(Icons.Rounded.ChevronRight, null, tint = AlmishkatTokens.MushafGold)
                    }
                    Text("$ayah", color = AlmishkatTokens.LightIvory,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { ayah++; refresh() }) {
                        Icon(Icons.Rounded.ChevronLeft, null, tint = AlmishkatTokens.MushafGold)
                    }
                }
            }
        }

        Surface(
            color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlmishkatTokens.GlassBorder)
        ) {
            Column(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 210.dp).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("سورة ${surahNames[surah-1]}  •  الآية $ayah",
                    color = AlmishkatTokens.MushafGold, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))
                if (record != null) {
                    Text(record!!.textUthmani, color = AlmishkatTokens.LightIvory,
                        fontSize = 27.sp, lineHeight = 52.sp, textAlign = TextAlign.Center)
                } else {
                    Text("لم تُثبّت بيانات المصحف الرسمية لهذه الرواية بعد",
                        color = AlmishkatTokens.TajweedAmber, fontSize = 15.sp,
                        textAlign = TextAlign.Center, lineHeight = 25.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("لن نعرض نصاً تجريبياً مكان الآية. أضف حزمة المصدر الرسمي الموثّقة لتفعيل القراءة دون إنترنت.",
                        color = AlmishkatTokens.TextSecondary, fontSize = 11.sp,
                        textAlign = TextAlign.Center, lineHeight = 19.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text(riwayah.arabicName, color = AlmishkatTokens.TextSecondary, fontSize = 10.sp)
            }
        }

        Surface(color = AlmishkatTokens.SanctuarySurface,
            shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("حالة المحتوى المحلي", color = AlmishkatTokens.MushafGold,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(status ?: "لا توجد حزمة مثبتة للرواية ${riwayah.arabicName}",
                    color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
                Text("قارئ المصحف جاهز للربط ببيانات المجمع الرسمية؛ لم يتم تضمين نصوص المصحف في هذه الحزمة.",
                    color = AlmishkatTokens.TajweedAmber, fontSize = 10.sp,
                    lineHeight = 17.sp)
            }
        }
    }
}
