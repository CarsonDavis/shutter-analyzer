package com.shutteranalyzer.analysis

import com.shutteranalyzer.analysis.model.ShutterEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of shutter speed calculation.
 */
data class ShutterSpeedResult(
    val event: ShutterEvent,
    val measuredSpeed: Double,
    val expectedSpeed: Double?,
    val errorPercent: Double?
)

/**
 * Calculates and analyzes shutter speeds.
 *
 * Ported from Python ShutterSpeedCalculator class in shutter_calculator.py:134-256
 */
@Singleton
class ShutterSpeedCalculator @Inject constructor() {

    companion object {
        /**
         * Static helper to calculate shutter speed from duration frames.
         *
         * @param durationFrames Number of frames (can be weighted)
         * @param fps Frames per second
         * @return Shutter speed as 1/x (e.g., 500.0 for 1/500 second)
         */
        fun calculateShutterSpeed(durationFrames: Double, fps: Double): Double {
            val duration = durationFrames / fps
            return 1.0 / duration
        }

        /**
         * Format shutter speed as a human-readable string.
         *
         * @param speed Shutter speed as 1/x value
         * @return Formatted string like "1/500" or "1/60"
         */
        fun formatShutterSpeed(speed: Double): String {
            return if (speed >= 1.0) {
                "1/${speed.toInt()}"
            } else {
                "${"%.2f".format(1.0 / speed)}s"
            }
        }
    }

    /**
     * Calculates the effective shutter speed as a fraction (1/x).
     *
     * @param shutterEvent ShutterEvent object
     * @param fps Frames per second of the saved video
     * @param recordingFps Actual recording FPS (for slow motion videos)
     * @param useWeighted If true, use weighted frame count for better accuracy
     * @return Shutter speed as 1/x (e.g., 500.0 for 1/500 second)
     */
    fun calculateShutterSpeed(
        shutterEvent: ShutterEvent,
        fps: Double,
        recordingFps: Double? = null,
        useWeighted: Boolean = true
    ): Double {
        // Get frame count (weighted or simple)
        val frameCount = if (useWeighted && shutterEvent.baselineBrightness != null) {
            shutterEvent.weightedDurationFrames
        } else {
            shutterEvent.durationFrames.toDouble()
        }

        // Use recordingFps if provided (for slow-motion videos)
        val effectiveFps = recordingFps ?: fps

        // Calculate duration in seconds
        val duration = frameCount / effectiveFps

        // Convert to conventional shutter speed (1/x)
        // Return the reciprocal of the duration
        return 1.0 / duration
    }

    /**
     * Calculate duration in seconds.
     *
     * @param startFrame Start frame index
     * @param endFrame End frame index
     * @param fps Frames per second of the saved video
     * @param recordingFps Actual recording FPS (for slow motion videos)
     * @return Duration in seconds
     */
    fun calculateDurationSeconds(
        startFrame: Int,
        endFrame: Int,
        fps: Double,
        recordingFps: Double? = null
    ): Double {
        val frameCount = endFrame - startFrame + 1

        return if (recordingFps != null) {
            // Adjust for slow motion recording
            val timeScaleFactor = fps / recordingFps
            (frameCount / fps) * timeScaleFactor
        } else {
            frameCount / fps
        }
    }

    /**
     * Compares measured vs. expected speeds.
     *
     * @param measured Measured shutter speed (1/x value, e.g., 500 for 1/500)
     * @param expected Expected shutter speed (1/x value)
     * @return Percentage error (positive means measured is faster than expected)
     */
    fun compareWithExpected(measured: Double, expected: Double): Double {
        return ((measured - expected) / expected) * 100
    }

    /**
     * Format shutter speed as a human-readable string.
     *
     * @param speed Shutter speed as 1/x value
     * @return Formatted string like "1/500" or "1/60"
     */
    fun formatShutterSpeed(speed: Double): String {
        return if (speed >= 1.0) {
            "1/${speed.toInt()}"
        } else {
            "${"%.2f".format(1.0 / speed)}s"
        }
    }

    /**
     * Groups events by expected speed settings.
     *
     * Sorts events by duration before matching with expected speeds.
     *
     * @param shutterEvents List of ShutterEvent objects
     * @param expectedSpeeds List of expected shutter speeds as 1/x values (from fastest to slowest)
     * @param fps Frames per second of the video
     * @param recordingFps Actual recording FPS (for slow motion videos)
     * @return Map of expected speeds to lists of ShutterSpeedResult
     */
    fun groupShutterEvents(
        shutterEvents: List<ShutterEvent>,
        expectedSpeeds: List<Double>,
        fps: Double,
        recordingFps: Double? = null
    ): Map<Double, List<ShutterSpeedResult>> {
        // Sort expected speeds from fastest to slowest (largest 1/x value to smallest)
        val expectedSpeedsSorted = expectedSpeeds.sorted().reversed()

        // Sort shutter events by duration (shortest to longest = fastest to slowest)
        val sortedEvents = shutterEvents.sortedBy { it.durationFrames }

        // Truncate lists to match
        val minLength = minOf(sortedEvents.size, expectedSpeedsSorted.size)
        val eventsToMatch = sortedEvents.take(minLength)
        val speedsToMatch = expectedSpeedsSorted.take(minLength)

        // Match events with speeds
        val result = mutableMapOf<Double, MutableList<ShutterSpeedResult>>()

        eventsToMatch.zip(speedsToMatch).forEach { (event, expected) ->
            val measured = calculateShutterSpeed(event, fps, recordingFps)
            val error = compareWithExpected(measured, expected)

            val speedResult = ShutterSpeedResult(
                event = event,
                measuredSpeed = measured,
                expectedSpeed = expected,
                errorPercent = error
            )

            result.getOrPut(expected) { mutableListOf() }.add(speedResult)
        }

        return result
    }

    /**
     * Calculate results for all events without grouping.
     *
     * @param shutterEvents List of ShutterEvent objects
     * @param fps Frames per second of the video
     * @param recordingFps Actual recording FPS (for slow motion videos)
     * @return List of ShutterSpeedResult (without expected speeds)
     */
    fun calculateAllSpeeds(
        shutterEvents: List<ShutterEvent>,
        fps: Double,
        recordingFps: Double? = null
    ): List<ShutterSpeedResult> {
        return shutterEvents.map { event ->
            val measured = calculateShutterSpeed(event, fps, recordingFps)
            ShutterSpeedResult(
                event = event,
                measuredSpeed = measured,
                expectedSpeed = null,
                errorPercent = null
            )
        }
    }
}
