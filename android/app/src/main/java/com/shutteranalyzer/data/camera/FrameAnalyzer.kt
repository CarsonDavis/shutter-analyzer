package com.shutteranalyzer.data.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import javax.inject.Inject

private const val TAG = "FrameAnalyzer"

/**
 * CameraX ImageAnalysis.Analyzer for real-time frame processing.
 *
 * Calculates brightness of each frame and passes it to the LiveEventDetector
 * for real-time event detection with two-phase calibration.
 */
class FrameAnalyzer @Inject constructor() : ImageAnalysis.Analyzer {

    private val liveEventDetector = LiveEventDetector()

    /** Callback when an event is detected */
    var onEventDetected: ((startTimestamp: Long, endTimestamp: Long, brightnessValues: List<Double>) -> Unit)? = null

    /** Callback when baseline calibration completes (Phase 1 - waiting for calibration shutter) */
    var onBaselineCalibrationComplete: ((baseline: Double) -> Unit)? = null

    /** Callback when full calibration completes (Phase 2 - ready to detect) */
    var onCalibrationComplete: ((baseline: Double, threshold: Double) -> Unit)? = null

    /** Callback for each frame's brightness (for UI display) */
    var onBrightnessUpdate: ((brightness: Double) -> Unit)? = null

    /** Callback for calibration progress updates */
    var onCalibrationProgress: ((progress: Float) -> Unit)? = null

    /** First frame timestamp - used as reference for frame index calculation */
    private var firstFrameTimestamp: Long = 0L

    /** Whether we've captured the first frame timestamp */
    private var hasFirstFrameTimestamp: Boolean = false

    /**
     * Get the first frame's timestamp (for use as recording start reference).
     * This uses the camera sensor's timestamp, not System.nanoTime().
     */
    val recordingStartTimestamp: Long
        get() = firstFrameTimestamp

    /**
     * Whether the analyzer is in baseline calibration phase.
     */
    val isCalibratingBaseline: Boolean
        get() = liveEventDetector.isCalibratingBaseline

    /**
     * Whether the analyzer is waiting for calibration shutter.
     */
    val isWaitingForCalibrationShutter: Boolean
        get() = liveEventDetector.isWaitingForCalibrationShutter

    /**
     * Whether calibration is complete (both phases).
     */
    val isCalibrated: Boolean
        get() = liveEventDetector.isCalibrated

    /**
     * Progress of calibration (0.0 to 1.0).
     * Baseline phase: 0.0 to 0.5
     * Waiting for calibration shutter: 0.5
     * Capturing calibration event: 0.75
     * Complete: 1.0
     */
    val calibrationProgress: Float
        get() = liveEventDetector.calibrationProgress

    /**
     * Whether an event is currently in progress.
     */
    val isEventInProgress: Boolean
        get() = liveEventDetector.isEventInProgress

    /**
     * Current threshold value (after full calibration).
     */
    val currentThreshold: Double
        get() = liveEventDetector.currentThreshold

    /**
     * Current baseline value (after baseline calibration).
     */
    val currentBaseline: Double
        get() = liveEventDetector.currentBaseline

    /**
     * Peak brightness from calibration event.
     */
    val currentPeak: Double
        get() = liveEventDetector.currentPeak

    /**
     * Number of events detected.
     * Note: The calibration shutter event is NOT counted.
     */
    val detectedEventCount: Int
        get() = liveEventDetector.detectedEventCount

    override fun analyze(image: ImageProxy) {
        try {
            // Calculate brightness from Y plane (luminance)
            val brightness = image.calculateBrightness()
            val timestamp = image.imageInfo.timestamp

            // Capture first frame timestamp as reference
            if (!hasFirstFrameTimestamp) {
                firstFrameTimestamp = timestamp
                hasFirstFrameTimestamp = true
                Log.d(TAG, "First frame timestamp captured: $firstFrameTimestamp")
            }

            // Update brightness callback
            onBrightnessUpdate?.invoke(brightness)

            // Update calibration progress during calibration phases
            if (!liveEventDetector.isCalibrated) {
                onCalibrationProgress?.invoke(liveEventDetector.calibrationProgress)
            }

            // Process frame through event detector
            when (val result = liveEventDetector.processFrame(brightness, timestamp)) {
                is EventResult.BaselineCalibrationComplete -> {
                    Log.d(TAG, "Baseline calibration complete. Baseline: ${liveEventDetector.currentBaseline}")
                    onBaselineCalibrationComplete?.invoke(liveEventDetector.currentBaseline)
                }
                is EventResult.CalibrationComplete -> {
                    Log.d(TAG, "Full calibration complete. Threshold: ${liveEventDetector.currentThreshold}, Peak: ${liveEventDetector.currentPeak}")
                    onCalibrationComplete?.invoke(
                        liveEventDetector.currentBaseline,
                        liveEventDetector.currentThreshold
                    )
                }
                is EventResult.EventDetected -> {
                    Log.d(TAG, "Event detected: ${result.startTimestamp} - ${result.endTimestamp}")
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
        } catch (e: Exception) {
            // Log but don't crash - camera frames can occasionally have issues
            Log.e(TAG, "Error analyzing frame: ${e.message}", e)
        } finally {
            // CRITICAL: Always close the image to release the buffer
            image.close()
        }
    }

    /**
     * Reset the analyzer and event detector for recalibration.
     * Does NOT reset firstFrameTimestamp - use resetForNewRecording() for that.
     */
    fun reset() {
        liveEventDetector.reset()
        // DO NOT reset firstFrameTimestamp here - it should persist across recalibrations
        // within the same recording session
    }

    /**
     * Reset for a completely new recording session.
     * This resets everything including the first frame timestamp.
     */
    fun resetForNewRecording() {
        liveEventDetector.reset()
        firstFrameTimestamp = 0L
        hasFirstFrameTimestamp = false
    }

    /**
     * Reset only events, keeping calibration and timestamp reference.
     */
    fun resetEvents() {
        liveEventDetector.resetEvents()
        // DO NOT reset firstFrameTimestamp - it should persist for the recording session
    }

    /**
     * Set calibration parameters.
     *
     * @param frameCount Number of frames to collect during baseline calibration
     * @param thresholdFactor Factor for final threshold (default 0.8 = 80% of peak)
     */
    fun setCalibrationParameters(frameCount: Int = 60, thresholdFactor: Double = 0.8) {
        liveEventDetector.calibrationFrameCount = frameCount
        liveEventDetector.thresholdFactor = thresholdFactor
    }
}
