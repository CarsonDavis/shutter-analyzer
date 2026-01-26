package com.shutteranalyzer.analysis.model

/**
 * Represents a single shutter opening/closing event.
 *
 * Ported from Python ShutterEvent class in shutter_calculator.py
 *
 * @property startFrame Frame index where the shutter starts opening
 * @property endFrame Frame index where the shutter finishes closing
 * @property brightnessValues List of brightness values for frames in the event
 * @property baselineBrightness Brightness when shutter is fully closed (for weighting)
 * @property peakBrightness Brightness when shutter is fully open (for weighting)
 */
data class ShutterEvent(
    val startFrame: Int,
    val endFrame: Int,
    val brightnessValues: List<Double>,
    val baselineBrightness: Double? = null,
    val peakBrightness: Double? = null,
    val expectedSpeed: String? = null,
    val measuredSpeed: Double? = null
) {
    /**
     * Calculate the duration of the event in frames.
     *
     * @return Number of frames the shutter was open
     */
    val durationFrames: Int
        get() = endFrame - startFrame + 1

    /**
     * Calculate weighted duration where partial-open frames contribute proportionally.
     *
     * Uses per-event peak calculation based on median brightness, which naturally
     * identifies the plateau level. This means:
     * - Plateau frames (at or above median) contribute 1.0
     * - Transition frames (below median) contribute proportionally
     *
     * Example: brightness values [20, 80, 80, 100, 100, 80, 20]
     * - Median = 80 (the plateau level)
     * - 20 → weight 0.25 (transition)
     * - 80, 100 → weight 1.0 (plateau/fully open)
     *
     * @return Weighted frame count (float)
     */
    val weightedDurationFrames: Double
        get() {
            if (brightnessValues.isEmpty()) {
                return durationFrames.toDouble()
            }

            if (baselineBrightness == null) {
                return durationFrames.toDouble()
            }

            // Use median of this event's brightness as the peak (plateau level)
            val eventPeak = brightnessValues.median()

            // Fall back if peak is not greater than baseline
            if (eventPeak <= baselineBrightness) {
                return durationFrames.toDouble()
            }

            val brightnessRange = eventPeak - baselineBrightness

            return brightnessValues.sumOf { brightness ->
                // Calculate weight: 0 at baseline, 1 at event's median (plateau)
                val weight = (brightness - baselineBrightness) / brightnessRange
                // Clamp to [0, 1] - frames above median also count as 1.0
                weight.coerceIn(0.0, 1.0)
            }
        }

    /**
     * Get the maximum brightness value during the event.
     *
     * @return Maximum brightness value
     */
    val maxBrightness: Double
        get() = brightnessValues.maxOrNull() ?: 0.0

    /**
     * Get the average brightness value during the event.
     *
     * @return Average brightness value
     */
    val avgBrightness: Double
        get() = if (brightnessValues.isNotEmpty()) {
            brightnessValues.average()
        } else {
            0.0
        }

    override fun toString(): String {
        return "ShutterEvent(startFrame=$startFrame, endFrame=$endFrame, " +
                "durationFrames=$durationFrames, weighted=${"%.2f".format(weightedDurationFrames)}, " +
                "maxBrightness=${"%.2f".format(maxBrightness)})"
    }
}

/**
 * Extension function to calculate median of a list of doubles.
 */
fun List<Double>.median(): Double {
    if (isEmpty()) return 0.0
    val sorted = sorted()
    val middle = size / 2
    return if (size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2.0
    } else {
        sorted[middle]
    }
}

/**
 * Extension function to calculate a specific percentile of a list of doubles.
 *
 * @param percentile The percentile to calculate (0-100)
 * @return The value at the specified percentile
 */
fun List<Double>.percentile(percentile: Int): Double {
    if (isEmpty()) return 0.0
    require(percentile in 0..100) { "Percentile must be between 0 and 100" }

    val sorted = sorted()
    val index = (percentile / 100.0) * (size - 1)
    val lower = index.toInt()
    val upper = (lower + 1).coerceAtMost(size - 1)
    val fraction = index - lower

    return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
}

/**
 * Extension function to calculate standard deviation of a list of doubles.
 */
fun List<Double>.stdDev(): Double {
    if (size < 2) return 0.0
    val mean = average()
    val variance = sumOf { (it - mean) * (it - mean) } / size
    return kotlin.math.sqrt(variance)
}
