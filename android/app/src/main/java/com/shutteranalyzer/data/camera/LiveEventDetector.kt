package com.shutteranalyzer.data.camera

import com.shutteranalyzer.analysis.model.median
import com.shutteranalyzer.analysis.model.percentile
import com.shutteranalyzer.analysis.model.stdDev

/**
 * State of the live event detector.
 *
 * Two-phase calibration flow:
 * Idle → CalibratingBaseline → WaitingForCalibrationShutter → CapturingCalibrationEvent → WaitingForEvent → EventInProgress
 */
sealed class DetectorState {
    /** Initial state: Waiting for user to start calibration */
    object Idle : DetectorState()

    /** Phase 1: Collecting baseline frames (dark, shutter closed) - 5 seconds time-based */
    object CalibratingBaseline : DetectorState()

    /** Phase 2: Baseline established, waiting for user to fire calibration shutter */
    object WaitingForCalibrationShutter : DetectorState()

    /** Phase 2b: Calibration shutter fired, capturing peak brightness */
    data class CapturingCalibrationEvent(
        val brightnessValues: MutableList<Double> = mutableListOf()
    ) : DetectorState()

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
    /** Phase 1 complete: baseline established, waiting for calibration shutter */
    object BaselineCalibrationComplete : EventResult()

    /** Full calibration complete: ready to detect actual events */
    object CalibrationComplete : EventResult()

    /** An event was detected (shutter opened and closed) */
    data class EventDetected(
        val startTimestamp: Long,
        val endTimestamp: Long,
        val brightnessValues: List<Double>
    ) : EventResult()
}

/**
 * Real-time event detector for live camera feed with two-phase calibration.
 *
 * Two-phase calibration:
 * 1. Baseline phase: Collect dark frames to establish baseline brightness
 * 2. Calibration shutter: User fires shutter once to capture peak brightness
 *    - This event is DISCARDED and not counted
 *    - Final threshold = baseline + (peak - baseline) * 0.8
 *
 * Usage:
 * 1. Create detector
 * 2. Call processFrame() for each camera frame
 * 3. Handle EventResult.BaselineCalibrationComplete → prompt user to fire calibration shutter
 * 4. Handle EventResult.CalibrationComplete → start prompting for actual speeds
 * 5. Handle EventResult.EventDetected → record actual events
 * 6. Call reset() to start over
 */
class LiveEventDetector {

    private var state: DetectorState = DetectorState.Idle
    private val calibrationFrames = mutableListOf<Double>()

    /** Timestamp when baseline calibration started (nanoseconds) */
    private var calibrationStartTimestamp: Long = 0L

    // Baseline calibration results
    private var baseline: Double = 0.0
    private var maxSeenDuringBaseline: Double = 0.0
    private var baselineStdDev: Double = 0.0
    private var preliminaryThreshold: Double = 0.0

    // Calibration event results
    private var calibrationPeak: Double = 0.0
    private var threshold: Double = 0.0

    private var eventCount = 0

    /**
     * Duration of baseline calibration in seconds.
     * Calibration is time-based to ensure consistent 5-second calibration regardless of frame rate.
     */
    var calibrationDurationSeconds: Double = CALIBRATION_DURATION_SECONDS

    /**
     * Factor for final threshold calculation.
     * threshold = baseline + (peak - baseline) * thresholdFactor
     */
    var thresholdFactor: Double = DEFAULT_THRESHOLD_FACTOR

    /**
     * Whether the detector is in idle state (waiting for calibration to start).
     */
    val isIdle: Boolean
        get() = state is DetectorState.Idle

    /**
     * Whether the detector is in baseline calibration phase.
     */
    val isCalibratingBaseline: Boolean
        get() = state is DetectorState.CalibratingBaseline

    /**
     * Whether the detector is waiting for calibration shutter.
     */
    val isWaitingForCalibrationShutter: Boolean
        get() = state is DetectorState.WaitingForCalibrationShutter

    /**
     * Whether the detector is capturing calibration event.
     */
    val isCapturingCalibrationEvent: Boolean
        get() = state is DetectorState.CapturingCalibrationEvent

    /**
     * Whether the detector has completed full calibration (both phases).
     */
    val isCalibrated: Boolean
        get() = state is DetectorState.WaitingForEvent || state is DetectorState.EventInProgress

    /**
     * Progress of calibration (0.0 to 1.0).
     * Idle: 0.0
     * Baseline phase: 0.0 to 0.5 (time-based)
     * Waiting for calibration shutter: 0.5
     * Capturing calibration event: 0.75
     * Complete: 1.0
     */
    val calibrationProgress: Float
        get() = when (state) {
            is DetectorState.Idle -> 0f
            is DetectorState.CalibratingBaseline -> {
                if (calibrationStartTimestamp == 0L) {
                    0f
                } else {
                    // Progress based on elapsed time
                    val elapsedNanos = System.nanoTime() - calibrationStartTimestamp
                    val elapsedSeconds = elapsedNanos / 1_000_000_000.0
                    (elapsedSeconds / calibrationDurationSeconds).coerceIn(0.0, 0.5).toFloat()
                }
            }
            is DetectorState.WaitingForCalibrationShutter -> 0.5f
            is DetectorState.CapturingCalibrationEvent -> 0.75f
            else -> 1.0f
        }

    /**
     * Whether an event is currently in progress.
     */
    val isEventInProgress: Boolean
        get() = state is DetectorState.EventInProgress

    /**
     * Current baseline brightness (after baseline calibration).
     */
    val currentBaseline: Double
        get() = baseline

    /**
     * Current threshold (after full calibration).
     */
    val currentThreshold: Double
        get() = threshold

    /**
     * Peak brightness from calibration event.
     */
    val currentPeak: Double
        get() = calibrationPeak

    /**
     * Number of events detected since last reset.
     * Note: The calibration shutter event is NOT counted.
     */
    val detectedEventCount: Int
        get() = eventCount

    /**
     * Process a frame and return any result.
     *
     * @param brightness Brightness value of the frame (0-255)
     * @param timestamp Timestamp of the frame in nanoseconds
     * @return EventResult if state changed, null otherwise
     */
    fun processFrame(brightness: Double, timestamp: Long): EventResult? {
        return when (val currentState = state) {
            is DetectorState.Idle -> {
                // Ignore frames when idle - waiting for user to start calibration
                null
            }

            is DetectorState.CalibratingBaseline -> {
                // Capture first frame timestamp if not set
                if (calibrationStartTimestamp == 0L) {
                    calibrationStartTimestamp = System.nanoTime()
                }

                calibrationFrames.add(brightness)

                // Check if calibration duration has elapsed (time-based)
                val elapsedNanos = System.nanoTime() - calibrationStartTimestamp
                val elapsedSeconds = elapsedNanos / 1_000_000_000.0

                if (elapsedSeconds >= calibrationDurationSeconds) {
                    finishBaselineCalibration()
                    state = DetectorState.WaitingForCalibrationShutter
                    EventResult.BaselineCalibrationComplete
                } else {
                    null
                }
            }

            is DetectorState.WaitingForCalibrationShutter -> {
                if (brightness > preliminaryThreshold) {
                    // Calibration shutter fired - start capturing
                    state = DetectorState.CapturingCalibrationEvent(
                        brightnessValues = mutableListOf(brightness)
                    )
                }
                null
            }

            is DetectorState.CapturingCalibrationEvent -> {
                if (brightness > preliminaryThreshold) {
                    // Still in calibration event
                    currentState.brightnessValues.add(brightness)
                    null
                } else {
                    // Calibration event ended - calculate final threshold
                    finishCalibrationEvent(currentState.brightnessValues)
                    state = DetectorState.WaitingForEvent
                    // Note: We do NOT increment eventCount - calibration event is discarded
                    EventResult.CalibrationComplete
                }
            }

            is DetectorState.WaitingForEvent -> {
                if (brightness > threshold) {
                    // Actual event started
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
     * Reset the detector to initial state (Idle).
     * Clears calibration and event history.
     */
    fun reset() {
        state = DetectorState.Idle
        calibrationFrames.clear()
        calibrationStartTimestamp = 0L
        baseline = 0.0
        maxSeenDuringBaseline = 0.0
        baselineStdDev = 0.0
        preliminaryThreshold = 0.0
        calibrationPeak = 0.0
        threshold = 0.0
        eventCount = 0
    }

    /**
     * Start baseline calibration from Idle state.
     * Transitions from Idle → CalibratingBaseline.
     *
     * @return true if calibration was started, false if not in Idle state
     */
    fun startBaselineCalibration(): Boolean {
        if (state !is DetectorState.Idle) {
            return false
        }
        state = DetectorState.CalibratingBaseline
        calibrationStartTimestamp = 0L // Will be set on first frame
        calibrationFrames.clear()
        return true
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

    /**
     * Finish baseline calibration (Phase 1).
     * Calculates baseline and preliminary threshold for detecting calibration shutter.
     */
    private fun finishBaselineCalibration() {
        if (calibrationFrames.isEmpty()) {
            baseline = 0.0
            preliminaryThreshold = MIN_BRIGHTNESS_INCREASE
            return
        }

        // Calculate baseline statistics
        baseline = calibrationFrames.percentile(25)
        maxSeenDuringBaseline = calibrationFrames.maxOrNull() ?: baseline
        baselineStdDev = calibrationFrames.stdDev()

        // Calculate preliminary threshold - must be robust to ambient noise
        // Use the maximum of three approaches to avoid false triggers:
        // 1. Statistical: 5 standard deviations above baseline
        // 2. Absolute: minimum brightness increase
        // 3. Adaptive: 2x the brightest noise seen during baseline
        preliminaryThreshold = maxOf(
            baseline + baselineStdDev * STDDEV_MULTIPLIER,
            baseline + MIN_BRIGHTNESS_INCREASE,
            maxSeenDuringBaseline * NOISE_MULTIPLIER
        )

        calibrationFrames.clear()
    }

    /**
     * Finish calibration event capture (Phase 2).
     * Calculates final threshold based on actual peak brightness.
     */
    private fun finishCalibrationEvent(brightnessValues: List<Double>) {
        if (brightnessValues.isEmpty()) {
            // Fallback if somehow empty
            calibrationPeak = preliminaryThreshold * 2
        } else {
            // Use maximum brightness as peak
            calibrationPeak = brightnessValues.maxOrNull() ?: preliminaryThreshold * 2
        }

        // Final threshold: 80% of the way from baseline to peak
        // Events must exceed this to be detected
        threshold = baseline + (calibrationPeak - baseline) * thresholdFactor
    }

    companion object {
        /** Default duration of baseline calibration in seconds */
        const val CALIBRATION_DURATION_SECONDS = 5.0

        /** Default threshold factor (80% of brightness range) */
        const val DEFAULT_THRESHOLD_FACTOR = 0.8

        /** Minimum brightness increase to trigger calibration event detection */
        const val MIN_BRIGHTNESS_INCREASE = 50.0

        /** Multiplier for max seen during baseline (for preliminary threshold) */
        const val NOISE_MULTIPLIER = 2.0

        /** Standard deviation multiplier (for preliminary threshold) */
        const val STDDEV_MULTIPLIER = 5.0
    }
}
