package com.shutteranalyzer.analysis

import com.shutteranalyzer.analysis.model.BrightnessStats
import com.shutteranalyzer.analysis.model.median
import com.shutteranalyzer.analysis.model.percentile
import com.shutteranalyzer.analysis.model.stdDev
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Method for calculating brightness threshold.
 */
enum class ThresholdMethod {
    /** Original method: baseline + (median - baseline) * factor */
    ORIGINAL,
    /** Z-score method: finds threshold closest to expected event count */
    ZSCORE
}

/**
 * Calculates brightness threshold for shutter detection.
 *
 * Ported from Python FrameAnalyzer.analyze_brightness_distribution() in frame_analyzer.py:66-139
 * and threshold calculation methods.
 */
@Singleton
class ThresholdCalculator @Inject constructor() {

    /**
     * Analyzes the brightness distribution of all frames to determine threshold.
     *
     * @param brightnessValues List of brightness values for all frames
     * @param percentileThreshold Percentile to use for baseline determination (default: 25)
     * @param marginFactor Factor to multiply with (median-baseline) to determine threshold (default: 1.5)
     * @param method Method to use for threshold calculation
     * @param expectedEventsCount Expected number of shutter events (needed for ZSCORE method)
     * @return BrightnessStats with calculated threshold
     */
    /**
     * Analyzes the brightness distribution of all frames to determine threshold.
     *
     * @param brightnessValues List of brightness values for all frames
     * @param percentileThreshold Percentile to use for baseline determination (default: 25)
     * @param marginFactor Factor to multiply with (median-baseline) to determine threshold (default: 1.5)
     * @param method Method to use for threshold calculation
     * @param expectedEventsCount Expected number of shutter events (needed for ZSCORE method)
     * @return BrightnessStats with calculated threshold, or null if input is empty
     */
    fun analyzeBrightnessDistribution(
        brightnessValues: List<Double>,
        percentileThreshold: Int = 25,
        marginFactor: Double = 1.5,
        method: ThresholdMethod = ThresholdMethod.ORIGINAL,
        expectedEventsCount: Int? = null
    ): BrightnessStats {
        if (brightnessValues.isEmpty()) {
            // Return sensible defaults for empty input
            return BrightnessStats(
                minBrightness = 0.0,
                maxBrightness = 0.0,
                meanBrightness = 0.0,
                medianBrightness = 0.0,
                percentiles = emptyMap(),
                baseline = 0.0,
                threshold = 0.0
            )
        }

        // Calculate statistics
        val minBrightness = brightnessValues.minOrNull()!!
        val maxBrightness = brightnessValues.maxOrNull()!!
        val meanBrightness = brightnessValues.average()
        val medianBrightness = brightnessValues.median()

        // Calculate percentiles
        val percentiles = mapOf(
            10 to brightnessValues.percentile(10),
            25 to brightnessValues.percentile(25),
            75 to brightnessValues.percentile(75),
            90 to brightnessValues.percentile(90)
        )

        // Determine baseline (closed shutter) brightness
        val baseline = percentiles[percentileThreshold]!!

        // Calculate threshold based on specified method
        val threshold = when (method) {
            ThresholdMethod.ZSCORE -> {
                if (expectedEventsCount != null) {
                    findThresholdUsingZscore(brightnessValues, expectedEventsCount)
                } else {
                    calculateOriginalThreshold(baseline, medianBrightness, maxBrightness, marginFactor)
                }
            }
            ThresholdMethod.ORIGINAL -> {
                calculateOriginalThreshold(baseline, medianBrightness, maxBrightness, marginFactor)
            }
        }

        return BrightnessStats(
            minBrightness = minBrightness,
            maxBrightness = maxBrightness,
            meanBrightness = meanBrightness,
            medianBrightness = medianBrightness,
            percentiles = percentiles,
            baseline = baseline,
            threshold = threshold
        )
    }

    /**
     * Calculate threshold using the original method.
     *
     * baseline + (median - baseline) * marginFactor
     * With fallback if brightness range is 0.
     */
    private fun calculateOriginalThreshold(
        baseline: Double,
        medianBrightness: Double,
        maxBrightness: Double,
        marginFactor: Double
    ): Double {
        val brightnessRange = medianBrightness - baseline
        var threshold = baseline + (brightnessRange * marginFactor)

        // Ensure threshold is not exactly equal to baseline if brightnessRange is 0
        if (threshold == baseline) {
            // Use a small percentage of the maximum brightness as threshold
            threshold = baseline + (maxBrightness - baseline) * 0.1
        }

        return threshold
    }

    /**
     * Find threshold using z-score to identify outliers.
     *
     * Ported from Python FrameAnalyzer.find_threshold_using_zscore() in frame_analyzer.py:205-230
     *
     * @param brightnessValues List of brightness values
     * @param expectedEventsCount Expected number of shutter events
     * @return Calculated threshold
     */
    fun findThresholdUsingZscore(
        brightnessValues: List<Double>,
        expectedEventsCount: Int
    ): Double {
        val mean = brightnessValues.average()
        val std = brightnessValues.stdDev()

        // If standard deviation is nearly zero, data is too uniform for z-score
        if (std < 1e-6) {
            return mean + 0.1
        }

        // Try different z-scores to find the one that gives closest to expected events
        var bestThreshold = mean
        var bestDiff = Double.MAX_VALUE

        // Try z-scores from 1.0 to 5.0 (40 steps)
        for (i in 0 until 40) {
            val z = 1.0 + (i * 4.0 / 39.0)  // Linear interpolation from 1.0 to 5.0
            val threshold = mean + (z * std)
            val eventsCount = brightnessValues.count { it > threshold }
            val diff = kotlin.math.abs(eventsCount - expectedEventsCount)

            if (diff < bestDiff) {
                bestDiff = diff.toDouble()
                bestThreshold = threshold
            }
        }

        return bestThreshold
    }
}
