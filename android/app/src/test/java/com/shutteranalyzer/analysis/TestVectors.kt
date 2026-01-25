package com.shutteranalyzer.analysis

/**
 * Test vectors for verifying algorithm parity with Python implementation.
 */
object TestVectors {

    /**
     * Simulated brightness values for a video with 3 shutter events.
     * Baseline ~20, peak ~180, with transitions.
     */
    val sampleBrightnessValues = listOf(
        // Closed shutter (frames 0-9)
        20.0, 21.0, 19.0, 20.0, 22.0, 18.0, 21.0, 20.0, 19.0, 20.0,
        // Event 1: Open transition → plateau → close transition (frames 10-24)
        40.0, 80.0, 140.0, 175.0, 180.0, 182.0, 180.0, 178.0, 180.0, 140.0, 80.0, 40.0, 20.0, 21.0, 19.0,
        // Closed shutter (frames 25-34)
        20.0, 18.0, 21.0, 20.0, 19.0, 22.0, 20.0, 18.0, 21.0, 20.0,
        // Event 2: Shorter event (frames 35-44)
        50.0, 120.0, 175.0, 180.0, 178.0, 140.0, 60.0, 21.0, 20.0, 19.0,
        // Closed shutter (frames 45-54)
        20.0, 21.0, 18.0, 20.0, 22.0, 19.0, 20.0, 21.0, 20.0, 18.0,
        // Event 3: Longer event (frames 55-79)
        30.0, 60.0, 100.0, 150.0, 175.0, 180.0, 182.0, 180.0, 181.0, 180.0,
        180.0, 179.0, 180.0, 181.0, 180.0, 178.0, 150.0, 100.0, 60.0, 30.0,
        // Closed shutter (frames 80-89)
        20.0, 21.0, 19.0, 20.0, 18.0, 21.0, 20.0, 19.0, 22.0, 20.0
    )

    /**
     * Expected threshold using original method with default parameters.
     * baseline (25th percentile) + (median - baseline) * 1.5
     */
    const val expectedThreshold = 50.0  // Approximate, actual may vary slightly

    /**
     * Known event frame ranges for sampleBrightnessValues with threshold ~50.
     */
    val expectedEvents = listOf(
        Triple(10, 21, 12),  // Event 1: start=10, end=21, frames=12
        Triple(35, 41, 7),   // Event 2: start=35, end=41, frames=7
        Triple(55, 78, 24)   // Event 3: start=55, end=78, frames=24
    )

    /**
     * Brightness values for a single shutter event with known weighted duration.
     * Used to verify weighted duration algorithm.
     */
    val weightedDurationTestEvent = listOf(
        20.0,   // 0.0 weight (at baseline)
        60.0,   // 0.5 weight (halfway)
        100.0,  // 1.0 weight (at plateau)
        100.0,  // 1.0 weight
        100.0,  // 1.0 weight
        60.0,   // 0.5 weight
        20.0    // 0.0 weight
    )
    const val weightedDurationBaseline = 20.0
    // Median = 60.0 (the middle value when sorted)
    // Actually median of [20,20,60,60,100,100,100] = 60
    // Weights: (20-20)/(60-20)=0, (60-20)/(60-20)=1, (100-20)/(60-20)=2 -> clamped to 1
    // Expected weighted = 0 + 1 + 1 + 1 + 1 + 1 + 0 = 5.0
    const val expectedWeightedDuration = 5.0

    /**
     * Test case for shutter speed calculation.
     * At 240fps, if an event lasts 4 frames, the shutter speed should be 1/60.
     */
    const val testFps = 240.0
    const val testEventFrames = 4
    const val expectedShutterSpeed = 60.0  // 1/60 second = 240/4
}
