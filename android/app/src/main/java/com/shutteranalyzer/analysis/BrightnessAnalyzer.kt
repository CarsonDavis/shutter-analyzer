package com.shutteranalyzer.analysis

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes frame brightness for shutter detection.
 *
 * Ported from Python FrameAnalyzer.calculate_frame_brightness() in frame_analyzer.py:44-64
 */
@Singleton
class BrightnessAnalyzer @Inject constructor() {

    /**
     * Calculates the overall brightness of a frame.
     *
     * The brightness is calculated as the average pixel value after converting to grayscale.
     *
     * @param frame A video frame (OpenCV Mat)
     * @return Average brightness value (0-255)
     */
    fun calculateBrightness(frame: Mat): Double {
        // Handle empty frames
        if (frame.empty()) {
            return 0.0
        }

        val gray = Mat()
        try {
            // Convert to grayscale if the frame is in color
            if (frame.channels() == 3 || frame.channels() == 4) {
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY)
            } else {
                frame.copyTo(gray)
            }

            // Calculate average pixel value
            val mean = Core.mean(gray)
            return mean.`val`[0]
        } finally {
            // CRITICAL: Release Mat to prevent memory leaks
            gray.release()
        }
    }

    /**
     * Calculates brightness values for a list of frames.
     *
     * @param frames List of video frames
     * @return List of brightness values
     */
    fun calculateBrightnessValues(frames: List<Mat>): List<Double> {
        return frames.map { calculateBrightness(it) }
    }

    /**
     * Determines if the shutter is open based on frame brightness.
     *
     * @param brightness The brightness value of the frame
     * @param threshold Brightness threshold for determining shutter state
     * @return True if the shutter is open (brightness > threshold), false otherwise
     */
    fun isShutterOpen(brightness: Double, threshold: Double): Boolean {
        return brightness > threshold
    }
}
