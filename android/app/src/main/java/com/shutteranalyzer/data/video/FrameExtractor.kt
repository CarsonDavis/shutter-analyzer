package com.shutteranalyzer.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FrameExtractor"

/**
 * Extracts frame thumbnails from videos for review screen display.
 *
 * Uses parallel extraction with multiple MediaMetadataRetriever instances
 * to speed up thumbnail loading (3-4x faster than sequential).
 */
@Singleton
class FrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Number of parallel extraction workers */
        private const val PARALLEL_WORKERS = 4
    }
    /**
     * Extract a frame from a video at a specific frame number.
     *
     * @param videoUri URI of the video file
     * @param frameNumber The frame number to extract (0-indexed)
     * @param fps Frames per second of the video
     * @param thumbnailWidth Target thumbnail width (height scaled proportionally)
     * @return Bitmap of the frame, or null if extraction fails
     */
    fun extractFrame(
        videoUri: Uri,
        frameNumber: Int,
        fps: Double,
        thumbnailWidth: Int = 120
    ): Bitmap? {
        if (fps <= 0) return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)

            // Convert frame number to timestamp in microseconds
            val timestampUs = ((frameNumber / fps) * 1_000_000).toLong()

            // Get frame at timestamp
            val frame = retriever.getFrameAtTime(
                timestampUs,
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: return null

            // Scale to thumbnail size
            val aspectRatio = frame.width.toFloat() / frame.height.toFloat()
            val thumbnailHeight = (thumbnailWidth / aspectRatio).toInt()

            Bitmap.createScaledBitmap(frame, thumbnailWidth, thumbnailHeight, true).also {
                if (it != frame) {
                    frame.recycle()
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Extract multiple frames in parallel using multiple MediaMetadataRetriever instances.
     *
     * Uses coroutines with a semaphore to limit concurrent extractors (each needs its own
     * MediaMetadataRetriever since they're not thread-safe).
     *
     * Performance: ~3-4x faster than sequential extraction.
     *
     * @param videoUri URI of the video file
     * @param frameNumbers List of frame numbers to extract
     * @param fps Frames per second of the video
     * @param thumbnailWidth Target thumbnail width
     * @return Map of frame number to Bitmap (missing frames won't be in the map)
     */
    suspend fun extractFrames(
        videoUri: Uri,
        frameNumbers: List<Int>,
        fps: Double,
        thumbnailWidth: Int = 120
    ): Map<Int, Bitmap> = coroutineScope {
        Log.d(TAG, "extractFrames called: uri=$videoUri, frames=${frameNumbers.size}, fps=$fps")

        if (fps <= 0 || frameNumbers.isEmpty()) {
            Log.w(TAG, "Invalid params: fps=$fps, frameCount=${frameNumbers.size}")
            return@coroutineScope emptyMap()
        }

        val startTime = System.currentTimeMillis()
        val uniqueFrames = frameNumbers.filter { it >= 0 }.distinct()

        // Use semaphore to limit concurrent MediaMetadataRetriever instances
        val semaphore = Semaphore(PARALLEL_WORKERS)

        val results = uniqueFrames.map { frameNumber ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    extractSingleFrame(videoUri, frameNumber, fps, thumbnailWidth)?.let {
                        frameNumber to it
                    }
                }
            }
        }.awaitAll().filterNotNull().toMap()

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Extracted ${results.size} of ${uniqueFrames.size} frames in ${elapsed}ms (${elapsed / uniqueFrames.size.coerceAtLeast(1)}ms/frame avg)")

        results
    }

    /**
     * Extract a single frame with its own MediaMetadataRetriever.
     * Thread-safe - can be called from multiple coroutines.
     */
    private fun extractSingleFrame(
        videoUri: Uri,
        frameNumber: Int,
        fps: Double,
        thumbnailWidth: Int
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)

            val timestampUs = ((frameNumber / fps) * 1_000_000).toLong()

            val frame = retriever.getFrameAtTime(
                timestampUs,
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: return null

            // Scale to thumbnail size
            val aspectRatio = frame.width.toFloat() / frame.height.toFloat()
            val thumbnailHeight = (thumbnailWidth / aspectRatio).toInt()

            Bitmap.createScaledBitmap(frame, thumbnailWidth, thumbnailHeight, true).also {
                if (it != frame) {
                    frame.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract frame $frameNumber: ${e.message}")
            null
        } finally {
            retriever.release()
        }
    }
}
