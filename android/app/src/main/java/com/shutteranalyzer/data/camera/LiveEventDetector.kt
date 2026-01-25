package com.shutteranalyzer.data.camera

import com.shutteranalyzer.analysis.model.median
import com.shutteranalyzer.analysis.model.percentile

/**
 * State of the live event detector.
 */
sealed class DetectorState {
    /** Collecting calibration frames to determine threshold */
    object Calibrating : DetectorState()

    /** Calibration complete, waiting for brightness to exceed threshold */
    object WaitingForEvent : DetectorState()

    /** Brightness exceeded threshold, event in progress */
    data class EventInProgress(
        val startTimestamp: Long,
        val brightnessValues: MutableList<Double> = mutableListOf()
    ) : DetectorState()
}

/**
 * Result of processing a frame.
 */
sealed class EventResult {
    /** Calibration just completed */
    object CalibrationComplete : EventResult()

    /** An event was detected (shutter opened and closed) */
    data class EventDetected(
        val startTimestamp: Long,
        val endTimestamp: Long,
        val brightnessValues: List<Double>
    ) : EventResult()
}

/**
 * Real-time event detector for live camera feed.
 *
 * Processes frames as they come in, detects when brightness exceeds
 * a dynamically calculated threshold, and reports events.
 *
 * Usage:
 * 1. Create detector
 * 2. Call processFrame() for each camera frame
 * 3. Handle EventResult when returned (calibration complete or event detected)
 * 4. Call reset() to start over
 */
class LiveEventDetector {

    private var state: DetectorState = DetectorState.Calibrating
    private val calibrationFrames = mutableListOf<Double>()

    private var baseline: Double = 0.0
    private var threshold: Double = 0.0

    private var eventCount = 0

    /**
     * Number of frames to collect during calibration.
     * At 60fps, this is ~1 second. At 120fps, ~0.5 seconds.
     */
    var calibrationFrameCount: Int = CALIBRATION_FRAME_COUNT

    /**
     * Margin factor for threshold calculation.
     * threshold = baseline + (median - baseline) * marginFactor
     */
    var marginFactor: Double = DEFAULT_MARGIN_FACTOR

    /**
     * Whether the detector is currently calibrating.
     */
    val isCalibrating: Boolean
        get() = state is DetectorState.Calibrating

    /**
     * Whether the detector has completed calibration.
     */
    val isCalibrated: Boolean
        get() = state !is DetectorState.Calibrating

    /**
     * Progress of calibration (0.0 to 1.0).
     */
    val calibrationProgress: Float
        get() = if (state is DetectorState.Calibrating) {
            calibrationFrames.size.toFloat() / calibrationFrameCount
        } else {
            1.0f
        }

    /**
     * Whether an event is currently in progress.
     */
    val isEventInProgress: Boolean
        get() = state is DetectorState.EventInProgress

    /**
     * Current baseline brightness (after calibration).
     */
    val currentBaseline: Double
        get() = baseline

    /**
     * Current threshold (after calibration).
     */
    val currentThreshold: Double
        get() = threshold

    /**
     * Number of events detected since last reset.
     */
    val detectedEventCount: Int
        get() = eventCount

    /**
     * Process a frame and return any result.
     *
     * @param brightness Brightness value of the frame (0-255)
     * @param timestamp Timestamp of the frame in nanoseconds
     * @return EventResult if calibration completed or event detected, null otherwise
     */
    fun processFrame(brightness: Double, timestamp: Long): EventResult? {
        return when (val currentState = state) {
            is DetectorState.Calibrating -> {
                calibrationFrames.add(brightness)
                if (calibrationFrames.size >= calibrationFrameCount) {
                    finishCalibration()
                    state = DetectorState.WaitingForEvent
                    EventResult.CalibrationComplete
                } else {
                    null
                }
            }

            is DetectorState.WaitingForEvent -> {
                if (brightness > threshold) {
                    // Event started
                    state = DetectorState.EventInProgress(
                        startTimestamp = timestamp,
                        brightnessValues = mutableListOf(brightness)
                    )
                }
                null
            }

            is DetectorState.EventInProgress -> {
                if (brightness > threshold) {
                    // Event continues
                    currentState.brightnessValues.add(brightness)
                    null
                } else {
                    // Event ended
                    eventCount++
                    state = DetectorState.WaitingForEvent
                    EventResult.EventDetected(
                        startTimestamp = currentState.startTimestamp,
                        endTimestamp = timestamp,
                        brightnessValues = currentState.brightnessValues.toList()
                    )
                }
            }
        }
    }

    /**
     * Reset the detector to initial state.
     * Clears calibration and event history.
     */
    fun reset() {
        state = DetectorState.Calibrating
        calibrationFrames.clear()
        baseline = 0.0
        threshold = 0.0
        eventCount = 0
    }

    /**
     * Reset only the event detection state, keeping calibration.
     * Useful for starting a new recording session without recalibrating.
     */
    fun resetEvents() {
        if (isCalibrated) {
            state = DetectorState.WaitingForEvent
            eventCount = 0
        }
    }

    private fun finishCalibration() {
        if (calibrationFrames.isEmpty()) {
            baseline = 0.0
            threshold = 50.0 // Default threshold
            return
        }

        // Calculate baseline as 25th percentile
        baseline = calibrationFrames.percentile(25)

        // Calculate threshold
        val median = calibrationFrames.median()
        val brightnessRange = median - baseline
        threshold = baseline + (brightnessRange * marginFactor)

        // Ensure threshold is not exactly baseline (for uniform brightness)
        if (threshold <= baseline) {
            val max = calibrationFrames.maxOrNull() ?: baseline
            threshold = baseline + (max - baseline) * 0.1
        }

        calibrationFrames.clear()
    }

    companion object {
        /** Default number of frames for calibration */
        const val CALIBRATION_FRAME_COUNT = 60

        /** Default margin factor for threshold */
        const val DEFAULT_MARGIN_FACTOR = 1.5
    }
}
