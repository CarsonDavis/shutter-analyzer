package com.shutteranalyzer.analysis

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for ThresholdCalculator.
 */
class ThresholdCalculatorTest {

    private lateinit var calculator: ThresholdCalculator

    @Before
    fun setUp() {
        calculator = ThresholdCalculator()
    }

    @Test
    fun `analyzeBrightnessDistribution calculates correct stats`() {
        val values = TestVectors.sampleBrightnessValues
        val stats = calculator.analyzeBrightnessDistribution(values)

        // Verify min and max
        assertEquals(values.minOrNull()!!, stats.minBrightness, 0.001)
        assertEquals(values.maxOrNull()!!, stats.maxBrightness, 0.001)

        // Verify mean
        assertEquals(values.average(), stats.meanBrightness, 0.001)

        // Verify percentiles are calculated
        assertTrue(stats.percentiles.containsKey(10))
        assertTrue(stats.percentiles.containsKey(25))
        assertTrue(stats.percentiles.containsKey(75))
        assertTrue(stats.percentiles.containsKey(90))

        // Verify baseline is 25th percentile
        assertEquals(stats.percentiles[25]!!, stats.baseline, 0.001)
    }

    @Test
    fun `threshold is above baseline`() {
        val stats = calculator.analyzeBrightnessDistribution(TestVectors.sampleBrightnessValues)
        assertTrue("Threshold should be above baseline", stats.threshold > stats.baseline)
    }

    @Test
    fun `threshold separates closed and open shutter frames`() {
        val stats = calculator.analyzeBrightnessDistribution(TestVectors.sampleBrightnessValues)

        // Most frames below 50 should be "closed" shutter
        val closedFrames = TestVectors.sampleBrightnessValues.count { it <= stats.threshold }
        val openFrames = TestVectors.sampleBrightnessValues.count { it > stats.threshold }

        // We expect more closed frames than open frames
        assertTrue("Should have more closed frames than open", closedFrames > openFrames)
    }

    @Test
    fun `zscore method finds reasonable threshold`() {
        val stats = calculator.analyzeBrightnessDistribution(
            brightnessValues = TestVectors.sampleBrightnessValues,
            method = ThresholdMethod.ZSCORE,
            expectedEventsCount = 3
        )

        // Threshold should still be reasonable
        assertTrue(stats.threshold > stats.baseline)
        assertTrue(stats.threshold < stats.maxBrightness)
    }

    @Test
    fun `uniform brightness uses fallback threshold`() {
        val uniformValues = List(100) { 50.0 }
        val stats = calculator.analyzeBrightnessDistribution(uniformValues)

        // Threshold should not equal baseline (fallback applied)
        assertTrue("Threshold should not equal baseline for uniform data",
            stats.threshold > stats.baseline || stats.threshold != 50.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty brightness values throws exception`() {
        calculator.analyzeBrightnessDistribution(emptyList())
    }

    @Test
    fun `different margin factors affect threshold`() {
        val stats1 = calculator.analyzeBrightnessDistribution(
            brightnessValues = TestVectors.sampleBrightnessValues,
            marginFactor = 1.0
        )
        val stats2 = calculator.analyzeBrightnessDistribution(
            brightnessValues = TestVectors.sampleBrightnessValues,
            marginFactor = 2.0
        )

        // Higher margin factor should give higher threshold
        assertTrue("Higher margin factor should give higher threshold",
            stats2.threshold > stats1.threshold)
    }
}
