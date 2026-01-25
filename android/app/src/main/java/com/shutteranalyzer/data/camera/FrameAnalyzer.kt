package com.shutteranalyzer.data.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import javax.inject.Inject

/**
 * CameraX ImageAnalysis.Analyzer for real-time frame processing.
 *
 * Calculates brightness of each frame and passes it to the LiveEventDetector
 * for real-time event detection.
 */
class FrameAnalyzer @Inject constructor() : ImageAnalysis.Analyzer {

    private val liveEventDetector = LiveEventDetector()

    /** Callback when an event is detected */
    var onEventDetected: ((startTimestamp: Long, endTimestamp: Long, brightnessValues: List<Double>) -> Unit)? = null

    /** Callback when calibration completes */
    var onCalibrationComplete: ((baseline: Double, threshold: Double) -> Unit)? = null

    /** Callback for each frame's brightness (for UI display) */
    var onBrightnessUpdate: ((brightness: Double) -> Unit)? = null

    /** Callback for calibration progress updates */
    var onCalibrationProgress: ((progress: Float) -> Unit)? = null

    /**
     * Whether the analyzer is currently calibrating.
     */
    val isCalibrating: Boolean
        get() = liveEventDetector.isCalibrating

    /**
     * Whether calibration is complete.
     */
    val isCalibrated: Boolean
        get() = liveEventDetector.isCalibrated

    /**
     * Progress of calibration (0.0 to 1.0).
     */
    val calibrationProgress: Float
        get() = liveEventDetector.calibrationProgress

    /**
     * Whether an event is currently in progress.
     */
    val isEventInProgress: Boolean
        get() = liveEventDetector.isEventInProgress

    /**
     * Current threshold value (after calibration).
     */
    val currentThreshold: Double
        get() = liveEventDetector.currentThreshold

    /**
     * Current baseline value (after calibration).
     */
    val currentBaseline: Double
        get() = liveEventDetector.currentBaseline

    /**
     * Number of events detected.
     */
    val detectedEventCount: Int
        get() = liveEventDetector.detectedEventCount

    override fun analyze(image: ImageProxy) {
        try {
            // Calculate brightness from Y plane (luminance)
            val brightness = image.calculateBrightness()
            val timestamp = image.imageInfo.timestamp

            // Update brightness callback
            onBrightnessUpdate?.invoke(brightness)

            // Update calibration progress if calibrating
            if (liveEventDetector.isCalibrating) {
                onCalibrationProgress?.invoke(liveEventDetector.calibrationProgress)
            }

            // Process frame through event detector
            when (val result = liveEventDetector.processFrame(brightness, timestamp)) {
                is EventResult.CalibrationComplete -> {
                    onCalibrationComplete?.invoke(
                        liveEventDetector.currentBaseline,
                        liveEventDetector.currentThreshold
                    )
                }
                is EventResult.EventDetected -> {
                    onEventDetected?.invoke(
                        result.startTimestamp,
                        result.endTimestamp,
                        result.brightnessValues
                    )
                }
                null -> {
                    // No event, continue processing
                }
            }
        } finally {
            // CRITICAL: Always close the image to release the buffer
            image.close()
        }
    }

    /**
     * Reset the analyzer and event detector.
     */
    fun reset() {
        liveEventDetector.reset()
    }

    /**
     * Reset only events, keeping calibration.
     */
    fun resetEvents() {
        liveEventDetector.resetEvents()
    }

    /**
     * Set calibration parameters.
     *
     * @param frameCount Number of frames to collect during calibration
     * @param marginFactor Margin factor for threshold calculation
     */
    fun setCalibrationParameters(frameCount: Int = 60, marginFactor: Double = 1.5) {
        liveEventDetector.calibrationFrameCount = frameCount
        liveEventDetector.marginFactor = marginFactor
    }
}
