package com.shutteranalyzer.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FrameExtractor"

/**
 * Extracts frame thumbnails from videos for review screen display.
 */
@Singleton
class FrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
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
     * Extract multiple frames efficiently using a single MediaMetadataRetriever instance.
     *
     * @param videoUri URI of the video file
     * @param frameNumbers List of frame numbers to extract
     * @param fps Frames per second of the video
     * @param thumbnailWidth Target thumbnail width
     * @return Map of frame number to Bitmap (missing frames won't be in the map)
     */
    fun extractFrames(
        videoUri: Uri,
        frameNumbers: List<Int>,
        fps: Double,
        thumbnailWidth: Int = 120
    ): Map<Int, Bitmap> {
        Log.d(TAG, "extractFrames called: uri=$videoUri, frames=${frameNumbers.size}, fps=$fps")

        if (fps <= 0 || frameNumbers.isEmpty()) {
            Log.w(TAG, "Invalid params: fps=$fps, frameCount=${frameNumbers.size}")
            return emptyMap()
        }

        val result = mutableMapOf<Int, Bitmap>()
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, videoUri)
            Log.d(TAG, "MediaMetadataRetriever opened successfully")

            for (frameNumber in frameNumbers) {
                val timestampUs = ((frameNumber / fps) * 1_000_000).toLong()

                val frame = retriever.getFrameAtTime(
                    timestampUs,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (frame == null) {
                    Log.w(TAG, "Failed to extract frame $frameNumber at ${timestampUs}us")
                    continue
                }

                // Scale to thumbnail size
                val aspectRatio = frame.width.toFloat() / frame.height.toFloat()
                val thumbnailHeight = (thumbnailWidth / aspectRatio).toInt()

                val thumbnail = Bitmap.createScaledBitmap(frame, thumbnailWidth, thumbnailHeight, true)
                if (thumbnail != frame) {
                    frame.recycle()
                }

                result[frameNumber] = thumbnail
            }

            Log.d(TAG, "Extracted ${result.size} of ${frameNumbers.size} frames")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting frames: ${e.message}", e)
        } finally {
            retriever.release()
        }

        return result
    }
}
