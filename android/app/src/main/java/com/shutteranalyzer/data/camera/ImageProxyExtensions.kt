package com.shutteranalyzer.data.camera

import androidx.camera.core.ImageProxy

/**
 * Sampling step for brightness calculation.
 * Higher values = faster but less accurate.
 * 4 means we sample every 4th pixel in both dimensions (1/16 of pixels).
 */
private const val SAMPLE_STEP = 4

/**
 * Calculate average brightness from ImageProxy efficiently.
 *
 * Uses only the Y (luminance) plane from YUV format, avoiding
 * full color conversion. Samples every Nth pixel for performance.
 *
 * @return Average brightness value (0-255)
 */
fun ImageProxy.calculateBrightness(): Double {
    // Safety check for planes
    if (planes.isEmpty()) return 0.0

    val yPlane = planes[0]
    val buffer = yPlane.buffer
    val pixelStride = yPlane.pixelStride
    val rowStride = yPlane.rowStride

    // Safety check for valid dimensions
    if (width <= 0 || height <= 0 || rowStride <= 0) return 0.0

    var sum = 0L
    var sampleCount = 0

    // Sample every SAMPLE_STEP pixels for performance
    for (y in 0 until height step SAMPLE_STEP) {
        for (x in 0 until width step SAMPLE_STEP) {
            val index = y * rowStride + x * pixelStride
            if (index >= 0 && index < buffer.capacity()) {
                sum += buffer.get(index).toInt() and 0xFF
                sampleCount++
            }
        }
    }

    return if (sampleCount > 0) {
        sum.toDouble() / sampleCount
    } else {
        0.0
    }
}

/**
 * Calculate brightness with a custom sampling rate.
 *
 * @param sampleStep Sample every Nth pixel (higher = faster, less accurate)
 * @return Average brightness value (0-255)
 */
fun ImageProxy.calculateBrightness(sampleStep: Int): Double {
    // Safety check for planes
    if (planes.isEmpty()) return 0.0

    val yPlane = planes[0]
    val buffer = yPlane.buffer
    val pixelStride = yPlane.pixelStride
    val rowStride = yPlane.rowStride

    // Safety check for valid dimensions
    if (width <= 0 || height <= 0 || rowStride <= 0) return 0.0

    var sum = 0L
    var sampleCount = 0

    for (y in 0 until height step sampleStep) {
        for (x in 0 until width step sampleStep) {
            val index = y * rowStride + x * pixelStride
            if (index >= 0 && index < buffer.capacity()) {
                sum += buffer.get(index).toInt() and 0xFF
                sampleCount++
            }
        }
    }

    return if (sampleCount > 0) {
        sum.toDouble() / sampleCount
    } else {
        0.0
    }
}

/**
 * Calculate brightness for a center region of the image.
 * Useful for focused measurements ignoring edge noise.
 *
 * @param centerFraction Fraction of image to use (0.5 = center 50%)
 * @return Average brightness value (0-255)
 */
fun ImageProxy.calculateCenterBrightness(centerFraction: Float = 0.5f): Double {
    // Safety check for planes
    if (planes.isEmpty()) return 0.0

    val yPlane = planes[0]
    val buffer = yPlane.buffer
    val pixelStride = yPlane.pixelStride
    val rowStride = yPlane.rowStride

    // Safety check for valid dimensions
    if (width <= 0 || height <= 0 || rowStride <= 0) return 0.0

    val marginX = ((1 - centerFraction) / 2 * width).toInt()
    val marginY = ((1 - centerFraction) / 2 * height).toInt()

    var sum = 0L
    var sampleCount = 0

    for (y in marginY until (height - marginY) step SAMPLE_STEP) {
        for (x in marginX until (width - marginX) step SAMPLE_STEP) {
            val index = y * rowStride + x * pixelStride
            if (index >= 0 && index < buffer.capacity()) {
                sum += buffer.get(index).toInt() and 0xFF
                sampleCount++
            }
        }
    }

    return if (sampleCount > 0) {
        sum.toDouble() / sampleCount
    } else {
        0.0
    }
}
