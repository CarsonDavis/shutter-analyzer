package com.shutteranalyzer.analysis

import com.shutteranalyzer.analysis.model.ShutterEvent
import com.shutteranalyzer.analysis.model.median
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Raw event data before conversion to ShutterEvent.
 */
data class RawShutterEvent(
    val startFrame: Int,
    val endFrame: Int,
    val brightnessValues: List<Double>
)

/**
 * Detects shutter events from brightness data.
 *
 * Ported from Python FrameAnalyzer.find_shutter_events() in frame_analyzer.py:155-203
 * and calculate_peak_brightness() in frame_analyzer.py:300-360
 */
@Singleton
class EventDetector @Inject constructor() {

    /**
     * Finds sequences of frames where the shutter is open.
     *
     * @param brightnessValues List of brightness values for each frame
     * @param threshold Brightness threshold for determining shutter state
     * @return List of RawShutterEvent representing shutter events
     */
    fun findShutterEvents(
        brightnessValues: List<Double>,
        threshold: Double
    ): List<RawShutterEvent> {
        val events = mutableListOf<RawShutterEvent>()
        var currentEventStart: Int? = null
        var currentBrightnessValues = mutableListOf<Double>()

        brightnessValues.forEachIndexed { frameIndex, brightness ->
            val isOpen = brightness > threshold

            when {
                // Shutter just opened
                isOpen && currentEventStart == null -> {
                    currentEventStart = frameIndex
                    currentBrightnessValues = mutableListOf(brightness)
                }
                // Shutter still open
                isOpen && currentEventStart != null -> {
                    currentBrightnessValues.add(brightness)
                }
                // Shutter just closed
                !isOpen && currentEventStart != null -> {
                    events.add(
                        RawShutterEvent(
                            startFrame = currentEventStart!!,
                            endFrame = frameIndex - 1,
                            brightnessValues = currentBrightnessValues.toList()
                        )
                    )
                    currentEventStart = null
                    currentBrightnessValues = mutableListOf()
                }
            }
        }

        // Handle case where video ends with shutter open
        if (currentEventStart != null) {
            events.add(
                RawShutterEvent(
                    startFrame = currentEventStart!!,
                    endFrame = brightnessValues.size - 1,
                    brightnessValues = currentBrightnessValues.toList()
                )
            )
        }

        return events
    }

    /**
     * Calculate the peak brightness from detected events using plateau analysis.
     *
     * Instead of using a simple percentile, this method:
     * 1. Identifies plateau frames within each event (frames >= 90% of event max)
     * 2. Only considers events with sufficient plateau frames (stable readings)
     * 3. Calculates the mean plateau brightness for each qualifying event
     * 4. Returns the median of these plateau means
     *
     * This gives a robust estimate of "fully open" brightness that isn't
     * skewed by outliers or short events that never fully stabilize.
     *
     * Ported from Python FrameAnalyzer.calculate_peak_brightness() in frame_analyzer.py:300-360
     *
     * @param events List of raw shutter events
     * @param plateauThreshold Fraction of event max to consider as plateau (default 0.90)
     * @param minPlateauFrames Minimum plateau frames for event to qualify (default 10)
     * @return Peak brightness value, or null if no events
     */
    fun calculatePeakBrightness(
        events: List<RawShutterEvent>,
        plateauThreshold: Double = 0.90,
        minPlateauFrames: Int = 10
    ): Double? {
        if (events.isEmpty()) {
            return null
        }

        val plateauMeans = mutableListOf<Double>()

        for (event in events) {
            if (event.brightnessValues.isEmpty()) {
                continue
            }

            val eventMax = event.brightnessValues.maxOrNull() ?: continue

            // Find plateau frames (within threshold of event max)
            val plateauValues = event.brightnessValues.filter { it >= eventMax * plateauThreshold }
            val plateauCount = plateauValues.size

            // Only use events with enough stable plateau frames
            if (plateauCount >= minPlateauFrames) {
                val plateauMean = plateauValues.average()
                plateauMeans.add(plateauMean)
            }
        }

        if (plateauMeans.isNotEmpty()) {
            // Use median of plateau means for robustness
            return plateauMeans.median()
        }

        // Fallback: if no events have enough plateau frames,
        // use 95th percentile of all event brightness values
        val allEventBrightness = events.flatMap { it.brightnessValues }
        if (allEventBrightness.isNotEmpty()) {
            val sorted = allEventBrightness.sorted()
            val index = (0.95 * (sorted.size - 1)).toInt()
            return sorted[index]
        }

        return null
    }

    /**
     * Convert raw events to ShutterEvent objects with baseline and peak brightness.
     *
     * @param rawEvents List of raw shutter events
     * @param baselineBrightness Baseline brightness (closed shutter)
     * @param peakBrightness Peak brightness (optional, will be calculated if null)
     * @return List of ShutterEvent objects
     */
    fun createShutterEvents(
        rawEvents: List<RawShutterEvent>,
        baselineBrightness: Double,
        peakBrightness: Double? = null
    ): List<ShutterEvent> {
        val actualPeak = peakBrightness ?: calculatePeakBrightness(rawEvents)

        return rawEvents.map { raw ->
            ShutterEvent(
                startFrame = raw.startFrame,
                endFrame = raw.endFrame,
                brightnessValues = raw.brightnessValues,
                baselineBrightness = baselineBrightness,
                peakBrightness = actualPeak
            )
        }
    }
}
