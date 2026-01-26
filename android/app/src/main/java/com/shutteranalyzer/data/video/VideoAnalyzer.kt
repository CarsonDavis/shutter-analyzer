package com.shutteranalyzer.data.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.shutteranalyzer.analysis.EventDetector
import com.shutteranalyzer.analysis.ThresholdCalculator
import com.shutteranalyzer.analysis.model.BrightnessStats
import com.shutteranalyzer.analysis.model.ShutterEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VideoAnalyzer"

/**
 * Information about a video file.
 */
data class VideoInfo(
    val uri: Uri,
    val durationMs: Long,
    val frameRate: Double,
    val width: Int,
    val height: Int
)

/**
 * Result of video analysis.
 */
data class AnalysisResult(
    val events: List<ShutterEvent>,
    val brightnessStats: BrightnessStats,
    val frameCount: Int
)

/**
 * Progress callback for video analysis.
 */
typealias ProgressCallback = (Float) -> Unit

/**
 * Analyzes video files to detect shutter events.
 *
 * Extracts frames from videos and uses the same brightness analysis
 * algorithms as the live recording feature.
 */
@Singleton
class VideoAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val thresholdCalculator: ThresholdCalculator,
    private val eventDetector: EventDetector
) {

    /**
     * Get video metadata without full analysis.
     *
     * @param uri URI of the video file
     * @return VideoInfo with metadata, or null if unable to read
     */
    suspend fun getVideoInfo(uri: Uri): VideoInfo? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return@withContext null

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0

            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            // Try to get frame rate - may not always be available
            val frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            val frameRate = frameRateStr?.toDoubleOrNull() ?: estimateFrameRate(durationMs, retriever)

            VideoInfo(
                uri = uri,
                durationMs = durationMs,
                frameRate = frameRate,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Estimate frame rate from video metadata.
     *
     * Tries multiple approaches:
     * 1. VIDEO_FRAME_COUNT / duration (most accurate when available)
     * 2. Falls back to 30fps (common for standard recordings)
     *
     * Note: For slow-motion videos (120, 240, 480 fps), the user should
     * manually specify the recording FPS in the import settings.
     */
    private fun estimateFrameRate(durationMs: Long, retriever: MediaMetadataRetriever): Double {
        // Try to get frame count (API 28+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val frameCount = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT
            )?.toIntOrNull()

            if (frameCount != null && frameCount > 0 && durationMs > 0) {
                val durationSeconds = durationMs / 1000.0
                val calculatedFps = frameCount / durationSeconds
                // Sanity check: fps should be between 1 and 960
                if (calculatedFps in 1.0..960.0) {
                    return calculatedFps
                }
            }
        }

        // Default to 30fps if we can't determine
        // In practice, slow-mo videos are typically 120, 240, or 480 fps
        // but standard recordings are 30 or 60 fps
        return 30.0
    }

    /**
     * Analyze a video to detect shutter events.
     *
     * Uses fast MediaCodec sequential decoding when possible, with
     * fallback to slower MediaMetadataRetriever if needed.
     *
     * @param uri URI of the video file
     * @param frameRate Recording frame rate (for accurate timing)
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return AnalysisResult with detected events, or null if analysis fails
     */
    suspend fun analyzeVideo(
        uri: Uri,
        frameRate: Double,
        onProgress: ProgressCallback? = null
    ): AnalysisResult? = withContext(Dispatchers.IO) {
        // Try fast MediaCodec path first
        try {
            val result = analyzeWithMediaCodec(uri, frameRate, onProgress)
            if (result != null) {
                Log.d(TAG, "MediaCodec analysis succeeded: ${result.frameCount} frames, ${result.events.size} events")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaCodec analysis failed, falling back to MediaMetadataRetriever", e)
        }

        // Fallback to slower but more compatible MediaMetadataRetriever
        Log.d(TAG, "Using MediaMetadataRetriever fallback")
        return@withContext analyzeWithMediaMetadataRetriever(uri, frameRate, onProgress)
    }

    /**
     * Fast analysis using MediaCodec sequential decoding.
     *
     * Performance: ~0.5-2ms per frame (vs 10-50ms for MediaMetadataRetriever)
     */
    private suspend fun analyzeWithMediaCodec(
        uri: Uri,
        frameRate: Double,
        onProgress: ProgressCallback?
    ): AnalysisResult? {
        val decoder = SequentialFrameDecoder(context, uri)

        try {
            if (!decoder.start()) {
                Log.w(TAG, "Failed to start MediaCodec decoder")
                return null
            }

            val brightnessValues = mutableListOf<Double>()
            var lastProgressUpdate = 0

            while (true) {
                val yPlane = decoder.decodeNextFrame() ?: break

                // Calculate brightness from Y-plane (luminance)
                val brightness = decoder.calculateBrightness(yPlane)
                brightnessValues.add(brightness)

                // Throttle progress updates to every 100 frames
                val frameCount = decoder.frameCount
                if (frameCount - lastProgressUpdate >= 100) {
                    onProgress?.invoke(decoder.getProgress())
                    lastProgressUpdate = frameCount
                }
            }

            if (brightnessValues.isEmpty()) {
                Log.w(TAG, "No frames decoded")
                return null
            }

            Log.d(TAG, "Decoded ${brightnessValues.size} frames via MediaCodec")

            // Calculate statistics and detect events
            val brightnessStats = thresholdCalculator.analyzeBrightnessDistribution(brightnessValues)
            val rawEvents = eventDetector.findShutterEvents(
                brightnessValues = brightnessValues,
                threshold = brightnessStats.threshold
            )
            val events = eventDetector.createShutterEvents(
                rawEvents = rawEvents,
                baselineBrightness = brightnessStats.baseline,
                peakBrightness = brightnessStats.peakBrightness
            )

            onProgress?.invoke(1.0f)

            return AnalysisResult(
                events = events,
                brightnessStats = brightnessStats,
                frameCount = brightnessValues.size
            )

        } finally {
            decoder.release()
        }
    }

    /**
     * Fallback analysis using MediaMetadataRetriever.
     *
     * Slower (10-50ms per frame) but more compatible with unusual video formats.
     */
    private suspend fun analyzeWithMediaMetadataRetriever(
        uri: Uri,
        frameRate: Double,
        onProgress: ProgressCallback?
    ): AnalysisResult? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: return null

            // Calculate approximate frame count
            val estimatedFrameCount = ((durationMs / 1000.0) * frameRate).toInt()

            // Sample frames at regular intervals
            val brightnessValues = mutableListOf<Double>()
            val sampleIntervalMs = (1000.0 / frameRate).toLong()

            var currentTimeMs = 0L
            var frameIndex = 0

            while (currentTimeMs < durationMs) {
                // Get frame at this timestamp
                val bitmap = retriever.getFrameAtTime(
                    currentTimeMs * 1000, // Convert to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap != null) {
                    // Calculate brightness from bitmap
                    val brightness = calculateBitmapBrightness(bitmap)
                    brightnessValues.add(brightness)
                    bitmap.recycle()
                }

                frameIndex++
                currentTimeMs += sampleIntervalMs

                // Report progress
                onProgress?.invoke(currentTimeMs.toFloat() / durationMs.toFloat())
            }

            if (brightnessValues.isEmpty()) {
                return null
            }

            // Calculate brightness statistics
            val brightnessStats = thresholdCalculator.analyzeBrightnessDistribution(brightnessValues)

            // Detect events using the same algorithm as live detection
            val rawEvents = eventDetector.findShutterEvents(
                brightnessValues = brightnessValues,
                threshold = brightnessStats.threshold
            )

            // Convert raw events to ShutterEvents
            val events = eventDetector.createShutterEvents(
                rawEvents = rawEvents,
                baselineBrightness = brightnessStats.baseline,
                peakBrightness = brightnessStats.peakBrightness
            )

            onProgress?.invoke(1.0f)

            return AnalysisResult(
                events = events,
                brightnessStats = brightnessStats,
                frameCount = brightnessValues.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "MediaMetadataRetriever analysis failed", e)
            return null
        } finally {
            retriever.release()
        }
    }

    /**
     * Calculate average brightness of a bitmap.
     */
    private fun calculateBitmapBrightness(bitmap: android.graphics.Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        var totalBrightness = 0L
        var sampleCount = 0

        // Sample every 4th pixel for performance
        val step = 4
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                // Convert to grayscale using luminance formula
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toLong()
                totalBrightness += luminance
                sampleCount++
            }
        }

        return if (sampleCount > 0) totalBrightness.toDouble() / sampleCount else 0.0
    }
}
