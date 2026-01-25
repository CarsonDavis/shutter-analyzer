package com.shutteranalyzer.analysis

import com.shutteranalyzer.analysis.model.ShutterEvent
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for ShutterEvent data class.
 */
class ShutterEventTest {

    @Test
    fun `durationFrames calculates correctly`() {
        val event = ShutterEvent(
            startFrame = 10,
            endFrame = 19,
            brightnessValues = List(10) { 100.0 }
        )
        assertEquals(10, event.durationFrames)
    }

    @Test
    fun `durationFrames single frame`() {
        val event = ShutterEvent(
            startFrame = 5,
            endFrame = 5,
            brightnessValues = listOf(100.0)
        )
        assertEquals(1, event.durationFrames)
    }

    @Test
    fun `weightedDurationFrames without baseline returns raw duration`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 4,
            brightnessValues = listOf(50.0, 100.0, 100.0, 100.0, 50.0),
            baselineBrightness = null
        )
        assertEquals(5.0, event.weightedDurationFrames, 0.001)
    }

    @Test
    fun `weightedDurationFrames with baseline calculates weights`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 6,
            brightnessValues = TestVectors.weightedDurationTestEvent,
            baselineBrightness = TestVectors.weightedDurationBaseline
        )
        assertEquals(TestVectors.expectedWeightedDuration, event.weightedDurationFrames, 0.001)
    }

    @Test
    fun `weightedDurationFrames clamps above-median to 1`() {
        // Values above the median should still count as 1.0
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 4,
            brightnessValues = listOf(20.0, 60.0, 200.0, 60.0, 20.0),  // 200 is above median of 60
            baselineBrightness = 20.0
        )
        // Median = 60
        // Weights: (20-20)/(60-20)=0, (60-20)/(60-20)=1, (200-20)/(60-20)=4.5 clamped to 1, etc.
        // Expected: 0 + 1 + 1 + 1 + 0 = 3.0
        assertEquals(3.0, event.weightedDurationFrames, 0.001)
    }

    @Test
    fun `weightedDurationFrames falls back when peak not above baseline`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 2,
            brightnessValues = listOf(20.0, 20.0, 20.0),  // All at baseline
            baselineBrightness = 20.0
        )
        // Should fall back to raw duration
        assertEquals(3.0, event.weightedDurationFrames, 0.001)
    }

    @Test
    fun `weightedDurationFrames with empty brightness returns raw`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 4,
            brightnessValues = emptyList(),
            baselineBrightness = 20.0
        )
        assertEquals(5.0, event.weightedDurationFrames, 0.001)
    }

    @Test
    fun `maxBrightness returns correct value`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 4,
            brightnessValues = listOf(50.0, 100.0, 150.0, 100.0, 50.0)
        )
        assertEquals(150.0, event.maxBrightness, 0.001)
    }

    @Test
    fun `maxBrightness empty list returns zero`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 0,
            brightnessValues = emptyList()
        )
        assertEquals(0.0, event.maxBrightness, 0.001)
    }

    @Test
    fun `avgBrightness calculates correctly`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 4,
            brightnessValues = listOf(50.0, 100.0, 150.0, 100.0, 50.0)
        )
        assertEquals(90.0, event.avgBrightness, 0.001)  // (50+100+150+100+50)/5
    }

    @Test
    fun `avgBrightness empty list returns zero`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 0,
            brightnessValues = emptyList()
        )
        assertEquals(0.0, event.avgBrightness, 0.001)
    }

    @Test
    fun `toString includes all relevant info`() {
        val event = ShutterEvent(
            startFrame = 10,
            endFrame = 19,
            brightnessValues = List(10) { 100.0 },
            baselineBrightness = 20.0
        )
        val str = event.toString()

        assertTrue(str.contains("startFrame=10"))
        assertTrue(str.contains("endFrame=19"))
        assertTrue(str.contains("durationFrames=10"))
        assertTrue(str.contains("maxBrightness"))
    }
}
