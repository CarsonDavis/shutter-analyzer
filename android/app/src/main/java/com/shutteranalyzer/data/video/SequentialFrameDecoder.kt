package com.shutteranalyzer.data.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer

private const val TAG = "SequentialFrameDecoder"

/**
 * Sequential frame decoder using MediaCodec for efficient video analysis.
 *
 * Significantly faster than MediaMetadataRetriever for sequential frame access:
 * - MediaMetadataRetriever: 10-50ms per frame (must seek each time)
 * - SequentialFrameDecoder: 0.5-2ms per frame (continuous decoding)
 *
 * Design decisions:
 * - Uses ByteBuffer output (not Surface) for direct access to pixel data
 * - Extracts only Y-plane (luminance) for brightness calculation
 * - Reuses byte array to minimize allocations
 * - Supports content:// URIs natively via MediaExtractor
 */
class SequentialFrameDecoder(
    private val context: Context,
    private val uri: Uri
) {
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var videoTrackIndex: Int = -1
    private var videoDurationUs: Long = 0L
    private var width: Int = 0
    private var height: Int = 0

    // Reusable buffer for Y-plane data
    private var yPlaneBuffer: ByteArray? = null

    // State tracking
    private var isStarted = false
    private var isInputEos = false
    private var isOutputEos = false
    private var decodedFrameCount = 0

    // Timeout for codec operations (microseconds)
    private val codecTimeoutUs = 10_000L

    /**
     * Video dimensions as (width, height).
     */
    val dimensions: Pair<Int, Int>
        get() = width to height

    /**
     * Video duration in microseconds.
     */
    val durationUs: Long
        get() = videoDurationUs

    /**
     * Number of frames decoded so far.
     */
    val frameCount: Int
        get() = decodedFrameCount

    /**
     * Initialize and start the decoder.
     *
     * @return true if initialization succeeded
     */
    fun start(): Boolean {
        try {
            // Initialize MediaExtractor
            extractor = MediaExtractor().apply {
                setDataSource(context, uri, null)
            }

            // Find video track
            val ext = extractor!!
            for (i in 0 until ext.trackCount) {
                val format = ext.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    ext.selectTrack(i)

                    // Extract video properties
                    width = format.getInteger(MediaFormat.KEY_WIDTH)
                    height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    videoDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        format.getLong(MediaFormat.KEY_DURATION)
                    } else {
                        0L
                    }

                    Log.d(TAG, "Video: ${width}x${height}, duration=${videoDurationUs}us, mime=$mime")

                    // Create and configure decoder
                    codec = MediaCodec.createDecoderByType(mime).apply {
                        // Configure for ByteBuffer output (no Surface)
                        configure(format, null, null, 0)
                        start()
                    }

                    break
                }
            }

            if (codec == null) {
                Log.e(TAG, "No video track found or decoder creation failed")
                release()
                return false
            }

            // Allocate reusable Y-plane buffer
            yPlaneBuffer = ByteArray(width * height)

            isStarted = true
            Log.d(TAG, "Decoder started successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start decoder: ${e.message}", e)
            release()
            return false
        }
    }

    /**
     * Decode the next frame and return Y-plane brightness values.
     *
     * @return ByteArray containing Y-plane (luminance) data, or null if no more frames
     */
    fun decodeNextFrame(): ByteArray? {
        if (!isStarted || isOutputEos) return null

        val codec = this.codec ?: return null
        val extractor = this.extractor ?: return null

        while (!isOutputEos) {
            // Feed input buffer if not at end of input stream
            if (!isInputEos) {
                val inputBufferIndex = codec.dequeueInputBuffer(codecTimeoutUs)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            // End of stream
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isInputEos = true
                            Log.d(TAG, "Input EOS reached")
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, sampleSize,
                                presentationTimeUs, 0
                            )
                            extractor.advance()
                        }
                    }
                }
            }

            // Get output buffer
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, codecTimeoutUs)

            when {
                outputBufferIndex >= 0 -> {
                    // Check for end of stream
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isOutputEos = true
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        Log.d(TAG, "Output EOS reached after $decodedFrameCount frames")
                        return null
                    }

                    // Get the output buffer
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null) {
                        val yPlane = extractYPlane(outputBuffer, bufferInfo)
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                        decodedFrameCount++
                        return yPlane
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)
                }

                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codec.outputFormat
                    Log.d(TAG, "Output format changed: $newFormat")
                    // Update dimensions if they changed
                    if (newFormat.containsKey(MediaFormat.KEY_WIDTH)) {
                        width = newFormat.getInteger(MediaFormat.KEY_WIDTH)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
                        height = newFormat.getInteger(MediaFormat.KEY_HEIGHT)
                    }
                    // Reallocate buffer if size changed
                    if (yPlaneBuffer?.size != width * height) {
                        yPlaneBuffer = ByteArray(width * height)
                    }
                }

                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // No output available yet, continue feeding input
                    if (isInputEos) {
                        // If input is done and no output, we might be done
                        continue
                    }
                }
            }
        }

        return null
    }

    /**
     * Extract Y-plane (luminance) from the output buffer.
     *
     * MediaCodec typically outputs in NV12 or NV21 format where:
     * - First width*height bytes are the Y plane (luminance)
     * - Remaining bytes are UV planes (chrominance) - we ignore these
     *
     * For brightness calculation, we only need the Y plane.
     */
    private fun extractYPlane(buffer: ByteBuffer, info: MediaCodec.BufferInfo): ByteArray {
        val yPlaneSize = width * height
        val yData = yPlaneBuffer ?: ByteArray(yPlaneSize).also { yPlaneBuffer = it }

        buffer.position(info.offset)

        // Read Y plane (first width*height bytes)
        val bytesToRead = minOf(buffer.remaining(), yPlaneSize)
        buffer.get(yData, 0, bytesToRead)

        // If we read less than expected, zero-fill the rest
        if (bytesToRead < yPlaneSize) {
            for (i in bytesToRead until yPlaneSize) {
                yData[i] = 0
            }
        }

        return yData
    }

    /**
     * Calculate average brightness from Y-plane data.
     *
     * @param yPlane ByteArray containing Y-plane luminance values (0-255)
     * @param sampleStep Sample every Nth pixel for performance (default 4 = 1/16 of pixels)
     * @return Average brightness value (0-255)
     */
    fun calculateBrightness(yPlane: ByteArray, sampleStep: Int = 4): Double {
        var sum = 0L
        var count = 0

        // Sample every sampleStep pixels
        var i = 0
        while (i < yPlane.size) {
            sum += yPlane[i].toInt() and 0xFF
            count++
            i += sampleStep
        }

        return if (count > 0) sum.toDouble() / count else 0.0
    }

    /**
     * Get current decode progress (0.0 to 1.0).
     */
    fun getProgress(): Float {
        val extractor = this.extractor ?: return 0f
        if (videoDurationUs <= 0) return 0f

        val currentTime = extractor.sampleTime
        return if (currentTime >= 0) {
            (currentTime.toFloat() / videoDurationUs).coerceIn(0f, 1f)
        } else if (isOutputEos) {
            1f
        } else {
            0f
        }
    }

    /**
     * Release all resources.
     */
    fun release() {
        try {
            codec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping codec: ${e.message}")
        }

        try {
            codec?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing codec: ${e.message}")
        }

        try {
            extractor?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing extractor: ${e.message}")
        }

        codec = null
        extractor = null
        yPlaneBuffer = null
        isStarted = false

        Log.d(TAG, "Decoder released, decoded $decodedFrameCount frames total")
    }
}
