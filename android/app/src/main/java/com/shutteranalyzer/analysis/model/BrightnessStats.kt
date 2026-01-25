package com.shutteranalyzer.analysis.model

/**
 * Data class to store brightness statistics for a video.
 *
 * Ported from Python FrameBrightnessStats dataclass in frame_analyzer.py
 *
 * @property minBrightness Minimum brightness value
 * @property maxBrightness Maximum brightness value
 * @property meanBrightness Mean brightness value
 * @property medianBrightness Median brightness value
 * @property percentiles Dictionary of brightness percentiles (e.g., {10: 23.5, 90: 187.2})
 * @property baseline The baseline brightness value (typically represents closed shutter)
 * @property threshold The calculated threshold to distinguish open/closed shutter
 * @property peakBrightness The typical brightness when shutter is fully open (95th percentile of events)
 */
data class BrightnessStats(
    val minBrightness: Double,
    val maxBrightness: Double,
    val meanBrightness: Double,
    val medianBrightness: Double,
    val percentiles: Map<Int, Double>,
    val baseline: Double,
    val threshold: Double,
    val peakBrightness: Double? = null
)
