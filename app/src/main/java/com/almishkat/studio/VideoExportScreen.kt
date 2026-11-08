    package com.almishkat.studio

    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import kotlinx.coroutines.launch

    @Composable
    fun VideoExportScreen() {
        val context = LocalContext.current
        val db = remember { QuranLocalDatabase(context) }
        val scope = rememberCoroutineScope()
        var clip by remember { mutableStateOf(db.getLatestAudioClipSelection()) }
        var state by remember { mutableStateOf<VideoExportState>(VideoExportState.Idle) }
        val createDocument = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("video/mp4")
        ) { uri ->
            if (uri != null) {
                val selected = clip
                if (selected == null) {
                    state = VideoExportState.Failure("احفظ نطاقاً صوتياً أولاً من محرر الموجة")
                } else {
                    scope.launch {
                        state = VideoExportState.Running(0f, "تهيئة محرك التصدير")
                        try {
                            val verse = if (selected.surahNumber != null && selected.ayahNumber != null) {
                                db.getAyah(selected.riwayah ?: QuranRiwayah.HAFS,
                                    selected.surahNumber, selected.ayahNumber)?.textUthmani.orEmpty()
                            } else ""
                            VideoExportEngine(context.contentResolver, context.applicationContext).export(
                                VideoExportRequest(
                                    audioUri = android.net.Uri.parse(selected.sourceUri),
                                    audioStartMs = selected.startMs,
                                    audioEndMs = selected.endMs,
                                    verseText = verse,
                                    surahNumber = selected.surahNumber,
                                    ayahNumber = selected.ayahNumber,
                                    riwayahName = selected.riwayah?.arabicName ?: "الرواية غير محددة",
                                    outputUri = uri,
                                    sceneLayers = selected.projectId?.let(db::listSceneLayers)
                                        ?.filter { it.visible }.orEmpty(),
                                    background = selected.projectId?.let(db::getSceneBackground),
                                    scenes = selected.projectId?.let(db::listStudioScenes)
                                        .orEmpty().sortedBy { it.order }.map { scene ->
                                            SceneRenderPlan(
                                                scene = scene,
                                                background = db.getSceneBackgroundForScene(scene.id)
                                                    ?: db.getSceneBackground(scene.projectId),
                                                layers = db.listSceneLayersForScene(scene.id).filter { it.visible }
                                            )
                                        }
                                )
                            ) { progress, message ->
                                state = VideoExportState.Running(progress, message)
                            }
                            state = VideoExportState.Success(uri)
                        } catch (t: Throwable) {
                            state = VideoExportState.Failure(t.localizedMessage ?: "فشل تصدير الفيديو")
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("تصدير الفيديو", color = AlmishkatTokens.LightIvory,
                fontSize = 19.sp)
            Text("إنشاء ملف MP4 عمودي بخلفية المشكاة والتلاوة المحددة.",
                color = AlmishkatTokens.TextSecondary, fontSize = 11.sp)
            Surface(color = AlmishkatTokens.SanctuarySurface,
                shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        clip?.let { "المقطع: ${QuranSyncEngine.formatTime(it.startMs)} – ${QuranSyncEngine.formatTime(it.endMs)}" }
                            ?: "لا يوجد نطاق صوتي محفوظ",
                        color = AlmishkatTokens.LightIvory, fontSize = 12.sp
                    )
                    Text(
                        clip?.let { "الرواية: ${it.riwayah?.arabicName ?: "غير محددة"} • السورة: ${it.surahNumber ?: "—"} • الآية: ${it.ayahNumber ?: "—"}" }
                            ?: "احفظ مقطعاً أولاً من تبويب الموجة الصوتية.",
                        color = AlmishkatTokens.TextSecondary, fontSize = 10.sp
                    )
                    Button(
                        onClick = { createDocument.launch("Almishkat_${System.currentTimeMillis()}.mp4") },
                        enabled = clip != null && state !is VideoExportState.Running,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("إنشاء فيديو MP4") }

                    when (val current = state) {
                        is VideoExportState.Running -> {
                            LinearProgressIndicator(
                                progress = { current.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(current.message, color = AlmishkatTokens.TextSecondary,
                                fontSize = 10.sp)
                        }
                        is VideoExportState.Success -> Text(
                            "اكتمل إنشاء الفيديو وحُفظ في الموقع الذي اخترته.",
                            color = AlmishkatTokens.TajweedSage, fontSize = 11.sp
                        )
                        is VideoExportState.Failure -> Text(
                            current.message, color = AlmishkatTokens.TajweedAmber,
                            fontSize = 11.sp
                        )
                        VideoExportState.Idle -> Text(
                            "سيتم إنشاء ملف MP4 دون الحاجة إلى الإنترنت.",
                            color = AlmishkatTokens.TextSecondary, fontSize = 10.sp
                        )
                    }
                }
            }
            Text(
                 "V18 يدعم صورة خلفية من الهاتف مع حركة تكبير بطيئة اختيارية، إضافة إلى قوالب لونية وطبقات النص. فيديو الخلفية والانتقالات بين المشاهد لم يضافا بعد.",
                color = AlmishkatTokens.TextSecondary, fontSize = 10.sp, lineHeight = 16.sp
            )
        }
    }
