    package com.almishkat.studio

    import android.content.ContentResolver
import android.content.Context
    import android.graphics.*
    import android.media.*
    import android.net.Uri
    import android.os.ParcelFileDescriptor
    import android.view.Surface
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.withContext
    import java.nio.ByteBuffer
    import java.util.concurrent.atomic.AtomicBoolean
    import kotlin.math.max
    import kotlin.math.min

    /**
     * Offline first-pass MP4 renderer:
     * - encodes a designed 9:16 canvas to H.264 using MediaCodec surface input
     * - copies the selected compressed audio range into the MP4 container
     * - no network dependency
     *
     * Supports ordered scene plans with per-scene image/video backgrounds and text layers.
     * The selected compressed audio range remains the single soundtrack.
     */
    class VideoExportEngine(
    private val resolver: ContentResolver,
    private val appContext: Context? = null
) {
        suspend fun export(
            request: VideoExportRequest,
            onProgress: (Float, String) -> Unit
        ) = withContext(Dispatchers.IO) {
            require(request.width > 0 && request.height > 0)
            require(request.width % 2 == 0 && request.height % 2 == 0)
            require(request.frameRate in 1..60)
            require(request.audioEndMs > request.audioStartMs) {
                "نطاق الصوت غير صالح"
            }

            val durationMs = request.audioEndMs - request.audioStartMs
            require(durationMs in 1L..300_000L) {
                "النسخة الحالية تحدّ مدة التصدير إلى خمس دقائق لحماية ذاكرة الهاتف"
            }
            val durationUs = durationMs * 1000L
            val totalFrames = max(1, ((durationUs / 1_000_000.0) * request.frameRate).toInt())
            val pfd = resolver.openFileDescriptor(request.outputUri, "w")
                ?: error("تعذر فتح ملف الإخراج")
            var encoder: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var inputSurface: Surface? = null
            data class SceneMedia(
                val background: SceneBackground?,
                var bitmap: Bitmap? = null,
                var retriever: MediaMetadataRetriever? = null,
                var durationUs: Long = 0L,
                var cachedFrame: Bitmap? = null,
                var cachedBucket: Long = -1L
            )
            val renderPlans = request.scenes
            val media = renderPlans.map { plan ->
                SceneMedia(plan.background).also { item ->
                    plan.background?.videoUri?.let { rawUri ->
                        appContext?.let { context ->
                            runCatching {
                                item.retriever = MediaMetadataRetriever().apply {
                                    setDataSource(context, Uri.parse(rawUri))
                                }
                                item.durationUs = item.retriever
                                    ?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    ?.toLongOrNull()?.times(1000L) ?: 0L
                            }.onFailure {
                                runCatching { item.retriever?.release() }
                                item.retriever = null
                            }
                        }
                    }
                    if (item.retriever == null) {
                        item.background?.imageUri?.let { rawUri ->
                            item.bitmap = resolver.openInputStream(Uri.parse(rawUri))?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                    }
                }
            }
            val fallbackMedia = if (renderPlans.isEmpty()) {
                SceneMedia(request.background).also { item ->
                    request.background?.videoUri?.let { rawUri ->
                        appContext?.let { context ->
                            runCatching {
                                item.retriever = MediaMetadataRetriever().apply {
                                    setDataSource(context, Uri.parse(rawUri))
                                }
                                item.durationUs = item.retriever
                                    ?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    ?.toLongOrNull()?.times(1000L) ?: 0L
                            }.onFailure {
                                runCatching { item.retriever?.release() }
                                item.retriever = null
                            }
                        }
                    }
                    if (item.retriever == null) {
                        item.background?.imageUri?.let { rawUri ->
                            item.bitmap = resolver.openInputStream(Uri.parse(rawUri))?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                    }
                }
            } else null
            val videoSamples = ArrayList<EncodedSample>(totalFrames.coerceAtMost(9000))
            try {
                muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                    request.width, request.height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, request.frameRate)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                }
                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = encoder.createInputSurface()
                encoder.start()

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create("serif", Typeface.BOLD)
                    textSize = request.width * .052f
                    setShadowLayer(7f, 0f, 3f, Color.BLACK)
                }
                val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(218, 181, 91)
                    textAlign = Paint.Align.CENTER
                    textSize = request.width * .026f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    letterSpacingCompat()
                }

                val info = MediaCodec.BufferInfo()
                val frameIntervalNs = 1_000_000_000L / request.frameRate
                val renderStartNs = System.nanoTime()
                for (frame in 0 until totalFrames) {
                    val targetNs = renderStartNs + frame * frameIntervalNs
                    val waitNs = targetNs - System.nanoTime()
                    if (waitNs > 0L) {
                        val waitMs = waitNs / 1_000_000L
                        val waitRemainderNs = (waitNs % 1_000_000L).toInt()
                        Thread.sleep(waitMs, waitRemainderNs)
                    }
                    val elapsedMs = frame * 1000L / request.frameRate
                    var activeIndex = 0
                    var sceneStartMs = 0L
                    var sceneEndMs = durationMs
                    var activePlan: SceneRenderPlan? = null
                    var activeMedia: SceneMedia? = fallbackMedia
                    if (renderPlans.isNotEmpty()) {
                        val totalSceneMs = renderPlans.sumOf { it.scene.durationMs.coerceAtLeast(1L) }
                        var cursor = 0L
                        val mappedTime = (elapsedMs.toDouble() / durationMs * totalSceneMs).toLong()
                            .coerceIn(0L, (totalSceneMs - 1L).coerceAtLeast(0L))
                        renderPlans.forEachIndexed { index, plan ->
                            val length = plan.scene.durationMs.coerceAtLeast(1L)
                            if (mappedTime >= cursor && mappedTime < cursor + length) {
                                activeIndex = index
                                sceneStartMs = (cursor.toDouble() / totalSceneMs * durationMs).toLong()
                                sceneEndMs = (((cursor + length).toDouble() / totalSceneMs) * durationMs).toLong()
                                activePlan = plan
                                activeMedia = media[index]
                            }
                            cursor += length
                        }
                    }
                    val planForFrame = activePlan
                    val activeRequest = if (planForFrame != null) request.copy(
                        verseText = planForFrame.layers.firstOrNull { it.type == SceneLayerType.QURAN }?.content
                            ?: request.verseText,
                        sceneLayers = planForFrame.layers.filter { it.visible }.map { layer ->
                            layer.copy(startMs = max(0L, layer.startMs), endMs = min(
                                layer.endMs, planForFrame.scene.durationMs
                            )).let { it.copy(startMs = it.startMs + sceneStartMs, endMs = it.endMs + sceneStartMs) }
                        },
                        background = planForFrame.background
                    ) else request
                    val sceneMedia = activeMedia
                    val videoFrame = sceneMedia?.retriever?.let { retriever ->
                        val localUs = ((elapsedMs - sceneStartMs).coerceAtLeast(0L) * 1000L)
                        val requestedUs = if (sceneMedia.durationUs > 0L)
                            localUs % sceneMedia.durationUs else localUs
                        val bucket = localUs / 200_000L
                        if (bucket != sceneMedia.cachedBucket) {
                            val decoded = runCatching {
                                retriever.getFrameAtTime(requestedUs, MediaMetadataRetriever.OPTION_CLOSEST)
                            }.getOrNull()
                            if (decoded != null) {
                                sceneMedia.cachedFrame?.let { if (!it.isRecycled) it.recycle() }
                                sceneMedia.cachedFrame = decoded
                                sceneMedia.cachedBucket = bucket
                            }
                        }
                        sceneMedia.cachedFrame
                    }
                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        val transitionMs = activePlan?.let { plan ->
                            val totalSceneMs = renderPlans.sumOf { it.scene.durationMs.coerceAtLeast(1L) }
                            (plan.scene.transitionDurationMs.toDouble() * durationMs / totalSceneMs)
                                .toLong().coerceAtMost((sceneEndMs - sceneStartMs).coerceAtLeast(0L))
                        } ?: 0L
                        val transitionProgress = if (transitionMs <= 0L) 1f else
                            ((elapsedMs - sceneStartMs).toFloat() / transitionMs).coerceIn(0f, 1f)
                        val isSlide = planForFrame?.scene?.transitionIn == SceneTransition.SLIDE_UP &&
                            transitionProgress < 1f
                        if (isSlide) {
                            canvas.save()
                            canvas.translate(0f, canvas.height * (1f - transitionProgress))
                        }
                        drawAlmishkatFrame(
                            canvas, activeRequest, frame, totalFrames,
                            paint, textPaint, goldPaint, videoFrame ?: sceneMedia?.bitmap,
                            activeRequest.background?.motionEnabled ?: true
                        )
                        if (isSlide) canvas.restore()
                        if (planForFrame != null && transitionProgress < 1f &&
                            planForFrame.scene.transitionIn in listOf(SceneTransition.FADE, SceneTransition.DISSOLVE)) {
                            val alpha = ((1f - transitionProgress) * 210f).toInt()
                            val transitionPaint = Paint().apply { color = Color.argb(alpha, 0, 0, 0) }
                            canvas.drawRect(0f, 0f, canvas.width.toFloat(),
                                canvas.height.toFloat(), transitionPaint)
                        }
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    drainEncoder(encoder, info, videoSamples, false)
                    if (frame % 15 == 0) {
                        onProgress(frame.toFloat() / totalFrames,
                            "تركيب الإطار ${frame + 1} من $totalFrames")
                    }
                }

                encoder.signalEndOfInputStream()
                var encoderDone = false
                while (!encoderDone) {
                    val outputIndex = encoder.dequeueOutputBuffer(info, 10_000)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                        outputIndex >= 0 -> {
                            val buffer = encoder.getOutputBuffer(outputIndex)
                            if (buffer != null && info.size > 0) {
                                val copy = ByteArray(info.size)
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                buffer.get(copy)
                                videoSamples += EncodedSample(copy, info.presentationTimeUs,
                                    info.flags)
                            }
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                                encoderDone = true
                            encoder.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
                val videoFormat = encoder.outputFormat
                val videoTrack = muxer.addTrack(videoFormat)

                val extractor = MediaExtractor()
                try {
                    val sourceDescriptor = resolver.openAssetFileDescriptor(request.audioUri, "r")
                        ?: error("تعذر فتح ملف الصوت المصدر")
                    try {
                        extractor.setDataSource(sourceDescriptor.fileDescriptor)
                    } finally {
                        sourceDescriptor.close()
                    }
                    var audioTrackSource = -1
                    var audioFormat: MediaFormat? = null
                    for (i in 0 until extractor.trackCount) {
                        val f = extractor.getTrackFormat(i)
                        if ((f.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
                            audioTrackSource = i
                            audioFormat = f
                            break
                        }
                    }
                    require(audioTrackSource >= 0 && audioFormat != null) {
                        "ملف المصدر لا يحتوي على مسار صوت مدعوم"
                    }
                    extractor.selectTrack(audioTrackSource)
                    val audioTrack = muxer.addTrack(audioFormat)
                    muxer.start()

                    val videoInfo = MediaCodec.BufferInfo()
                    videoSamples.forEach { sample ->
                        val data = ByteBuffer.wrap(sample.data)
                        videoInfo.set(0, sample.data.size, sample.ptsUs, sample.flags)
                        muxer.writeSampleData(videoTrack, data, videoInfo)
                    }

                    val audioBuffer = ByteBuffer.allocateDirect(1024 * 1024)
                    var writtenAudio = 0
                    while (true) {
                        audioBuffer.clear()
                        val size = extractor.readSampleData(audioBuffer, 0)
                        if (size < 0) break
                        val sourceTimeUs = extractor.sampleTime
                        if (sourceTimeUs >= request.audioEndMs * 1000L) break
                        if (sourceTimeUs >= request.audioStartMs * 1000L) {
                            val pts = sourceTimeUs - request.audioStartMs * 1000L
                            val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                                MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                            val audioInfo = MediaCodec.BufferInfo().apply {
                                set(0, size, pts, flags)
                            }
                            muxer.writeSampleData(audioTrack, audioBuffer, audioInfo)
                            writtenAudio++
                        }
                        extractor.advance()
                    }
                    require(writtenAudio > 0) { "لم يتم العثور على عينات صوت داخل النطاق المحدد" }
                } finally {
                    extractor.release()
                }
                onProgress(1f, "اكتمل إنشاء ملف MP4")
            } finally {
                runCatching { encoder?.stop() }
                runCatching { encoder?.release() }
                runCatching { inputSurface?.release() }
                runCatching { muxer?.stop() }
                runCatching { muxer?.release() }
                (media + listOfNotNull(fallbackMedia)).forEach { item ->
                    runCatching { item.bitmap?.recycle() }
                    runCatching { item.cachedFrame?.recycle() }
                    runCatching { item.retriever?.release() }
                }
                runCatching { pfd.close() }
            }
        }

        private data class EncodedSample(
            val data: ByteArray,
            val ptsUs: Long,
            val flags: Int
        )

        private fun drainEncoder(
            encoder: MediaCodec,
            info: MediaCodec.BufferInfo,
            samples: MutableList<EncodedSample>,
            endOfStream: Boolean
        ) {
            while (true) {
                val index = encoder.dequeueOutputBuffer(info, 0)
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) return
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) continue
                if (index >= 0) {
                    val buffer = encoder.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) {
                        val copy = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        buffer.get(copy)
                        samples += EncodedSample(copy, info.presentationTimeUs, info.flags)
                    }
                    val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    encoder.releaseOutputBuffer(index, false)
                    if (eos || endOfStream && eos) return
                } else return
            }
        }

        private fun drawAlmishkatFrame(
            canvas: Canvas,
            request: VideoExportRequest,
            frame: Int,
            totalFrames: Int,
            backgroundPaint: Paint,
            textPaint: Paint,
            goldPaint: Paint,
            backgroundBitmap: Bitmap?,
            motionEnabled: Boolean
        ) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            canvas.drawColor(Color.rgb(4, 37, 31))
            if (backgroundBitmap != null && !backgroundBitmap.isRecycled) {
                val scaleBase = max(w / backgroundBitmap.width, h / backgroundBitmap.height)
                val motion = if (motionEnabled) 1f + .045f * (frame.toFloat() / max(1, totalFrames - 1)) else 1f
                val drawWidth = backgroundBitmap.width * scaleBase * motion
                val drawHeight = backgroundBitmap.height * scaleBase * motion
                val left = (w - drawWidth) / 2f
                val top = (h - drawHeight) / 2f
                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(backgroundBitmap, null,
                    RectF(left, top, left + drawWidth, top + drawHeight), imagePaint)
                backgroundPaint.shader = null
                backgroundPaint.color = Color.argb(95, 2, 22, 20)
                canvas.drawRect(0f, 0f, w, h, backgroundPaint)
                backgroundPaint.color = Color.WHITE
            } else {
                canvas.drawColor(Color.rgb(4, 37, 31))
                backgroundPaint.shader = LinearGradient(0f, 0f, w, h,
                intArrayOf(Color.rgb(7, 66, 53), Color.rgb(5, 35, 31), Color.rgb(9, 20, 24)),
                null, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, backgroundPaint)
                backgroundPaint.shader = null
            }

            val centerX = w / 2f
            val progress = frame.toFloat() / max(1, totalFrames - 1)
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((20 + 20 * kotlin.math.sin(progress * Math.PI)).toInt(),
                    218, 181, 91)
            }
            canvas.drawCircle(centerX, h * .47f, w * .38f, glow)

            val arch = Path().apply {
                moveTo(w * .16f, h * .70f)
                lineTo(w * .16f, h * .38f)
                cubicTo(w * .16f, h * .15f, w * .84f, h * .15f, w * .84f, h * .38f)
                lineTo(w * .84f, h * .70f)
            }
            val archPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(80, 218, 181, 91)
                style = Paint.Style.STROKE
                strokeWidth = w * .009f
            }
            canvas.drawPath(arch, archPaint)

            goldPaint.textSize = w * .026f
            canvas.drawText("ALMISHKAT  •  المشكاة", centerX, h * .12f, goldPaint)
            canvas.drawText(
                if (request.surahNumber != null && request.ayahNumber != null)
                    "سورة ${request.surahNumber}  •  الآية ${request.ayahNumber}"
                else "آيات من القرآن الكريم",
                centerX, h * .79f, goldPaint
            )
            canvas.drawText(request.riwayahName, centerX, h * .83f, goldPaint)

            textPaint.textSize = w * .052f
            if (request.sceneLayers.isEmpty()) {
                drawArabicCentered(canvas, request.verseText.ifBlank { " " },
                    centerX, h * .49f, w * .72f, textPaint)
            } else {
                request.sceneLayers
                    .filter { it.visible && (frame * 1000L / request.frameRate) in it.startMs..it.endMs }
                    .sortedBy { it.zOrder }
                    .forEach { layer ->
                        val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                            val elapsedMs = frame * 1000L / request.frameRate
                            val fadeIn = ((elapsedMs - layer.startMs).toFloat() / 400f).coerceIn(0f, 1f)
                            val fadeOut = ((layer.endMs - elapsedMs).toFloat() / 400f).coerceIn(0f, 1f)
                            val alpha = min(fadeIn, fadeOut)
                            color = if (layer.type == SceneLayerType.DECORATION)
                                Color.argb((alpha * 255).toInt(), 218, 181, 91)
                            else Color.argb((alpha * 255).toInt(), 255, 255, 255)
                            textAlign = Paint.Align.CENTER
                            typeface = if (layer.type == SceneLayerType.QURAN)
                                Typeface.create("serif", Typeface.NORMAL)
                            else Typeface.create("sans-serif", Typeface.BOLD)
                            textSize = (layer.fontSize / 1080f * w).coerceIn(w * .018f, w * .09f)
                            setShadowLayer(5f, 0f, 2f, Color.BLACK)
                        }
                        canvas.drawText(
                            layer.content.take(180),
                            layer.positionX.coerceIn(0f, 1f) * w,
                            layer.positionY.coerceIn(.08f, .94f) * h,
                            layerPaint
                        )
                    }
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(170, 218, 181, 91)
                strokeWidth = 2f
            }
            canvas.drawLine(w * .30f, h * .88f, w * .70f, h * .88f, linePaint)
        }

        private fun drawArabicCentered(
            canvas: Canvas,
            text: String,
            centerX: Float,
            centerY: Float,
            maxWidth: Float,
            paint: Paint
        ) {
            val words = text.split(Regex("\\s+"))
            val lines = mutableListOf<String>()
            var current = ""
            words.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                    lines += current
                    current = word
                } else current = candidate
            }
            if (current.isNotEmpty()) lines += current
            val lineHeight = paint.textSize * 1.8f
            val firstBaseline = centerY - (lines.size - 1) * lineHeight / 2f
            lines.forEachIndexed { i, line ->
                canvas.drawText(line, centerX, firstBaseline + i * lineHeight, paint)
            }
        }

        private fun Paint.letterSpacingCompat() {
            // Paint has no letterSpacing property; spacing is handled by the text renderer.
        }
    }
