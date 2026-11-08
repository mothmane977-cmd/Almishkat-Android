package com.almishkat.studio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

data class WaveformData(
    val durationMs: Long,
    val peaks: List<Float>,
    val sampleRate: Int,
    val channelCount: Int
)

/**
 * Decodes an audio URI to PCM and reduces it to normalized peak buckets.
 * This runs on a worker thread; callers should not invoke it on the UI thread.
 */
object AudioWaveformAnalyzer {
    fun analyze(
        context: Context,
        uri: Uri,
        bucketCount: Int = 240
    ): WaveformData {
        require(bucketCount in 32..2000) { "bucketCount must be between 32 and 2000" }

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrack = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    inputFormat = format
                    break
                }
            }
            require(audioTrack >= 0 && inputFormat != null) { "No audio track found" }
            extractor.selectTrack(audioTrack)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION))
                inputFormat.getLong(MediaFormat.KEY_DURATION) else 0L
            val durationMs = durationUs / 1000L
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val outputPeaks = FloatArray(bucketCount)
            var totalFrames = 0L
            var inputDone = false
            var outputDone = false
            val info = MediaCodec.BufferInfo()
            var outputSampleRate = sampleRate
            var outputChannels = channels

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val size = extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime.coerceAtLeast(0L)
                            codec.queueInputBuffer(inputIndex, 0, size, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = codec.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                            outputSampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                            outputChannels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val buffer = codec.getOutputBuffer(outputIndex)
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val pcm = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
                            val bytesPerSample = when (
                                codec.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING,
                                    android.media.AudioFormat.ENCODING_PCM_16BIT)
                            ) {
                                android.media.AudioFormat.ENCODING_PCM_FLOAT -> 4
                                android.media.AudioFormat.ENCODING_PCM_8BIT -> 1
                                android.media.AudioFormat.ENCODING_PCM_16BIT -> 2
                                android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
                                android.media.AudioFormat.ENCODING_PCM_32BIT -> 4
                                else -> 2
                            }
                            val frameBytes = max(1, outputChannels * bytesPerSample)
                            val frames = info.size / frameBytes
                            for (frame in 0 until frames) {
                                var peak = 0f
                                for (channel in 0 until outputChannels) {
                                    val offset = frame * frameBytes + channel * bytesPerSample
                                    val value = when (bytesPerSample) {
                                        1 -> ((pcm.get(offset).toInt() and 0xff) - 128) / 128f
                                        2 -> pcm.getShort(offset).toFloat() / 32768f
                                        3 -> {
                                            val b0 = pcm.get(offset).toInt() and 0xff
                                            val b1 = pcm.get(offset + 1).toInt() and 0xff
                                            var b2 = pcm.get(offset + 2).toInt()
                                            var v = (b2 shl 16) or (b1 shl 8) or b0
                                            if (v and 0x800000 != 0) v = v or -0x1000000
                                            v / 8388608f
                                        }
                                        else -> pcm.getFloat(offset).coerceIn(-1f, 1f)
                                    }
                                    peak = max(peak, abs(value))
                                }
                                val bucket = ((totalFrames.toDouble() /
                                    max(1.0, durationMs.toDouble() * outputSampleRate / 1000.0)) *
                                    bucketCount).toInt().coerceIn(0, bucketCount - 1)
                                outputPeaks[bucket] = max(outputPeaks[bucket], peak)
                                totalFrames++
                            }
                        }
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                            outputDone = true
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            val normalized = outputPeaks.toList()
            return WaveformData(durationMs, normalized, outputSampleRate, outputChannels)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
