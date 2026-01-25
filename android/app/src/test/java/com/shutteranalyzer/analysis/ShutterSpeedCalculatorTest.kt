package com.shutteranalyzer.analysis

import com.shutteranalyzer.analysis.model.ShutterEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for ShutterSpeedCalculator.
 */
class ShutterSpeedCalculatorTest {

    private lateinit var calculator: ShutterSpeedCalculator

    @Before
    fun setUp() {
        calculator = ShutterSpeedCalculator()
    }

    @Test
    fun `calculateShutterSpeed with simple event`() {
        // At 240fps, 4 frames = 1/60 second
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 3,
            brightnessValues = listOf(100.0, 100.0, 100.0, 100.0),
            baselineBrightness = null  // No weighting
        )

        val speed = calculator.calculateShutterSpeed(event, fps = 240.0, useWeighted = false)
        assertEquals(60.0, speed, 0.001)
    }

    @Test
    fun `calculateShutterSpeed with weighted duration`() {
        // Event with transitions
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 6,
            brightnessValues = TestVectors.weightedDurationTestEvent,
            baselineBrightness = TestVectors.weightedDurationBaseline
        )

        // Verify weighted duration
        assertEquals(TestVectors.expectedWeightedDuration, event.weightedDurationFrames, 0.001)

        // At 240fps, 5 weighted frames = 1/48 second
        val speed = calculator.calculateShutterSpeed(event, fps = 240.0, useWeighted = true)
        assertEquals(48.0, speed, 0.001)
    }

    @Test
    fun `calculateShutterSpeed unweighted vs weighted`() {
        val event = ShutterEvent(
            startFrame = 0,
            endFrame = 6,
            brightnessValues = TestVectors.weightedDurationTestEvent,
            baselineBrightness = TestVectors.weightedDurationBaseline
        )

        val unweightedSpeed = calculator.calculateShutterSpeed(event, fps = 240.0, useWeighted = false)
        val weightedSpeed = calculator.calculateShutterSpeed(event, fps = 240.0, useWeighted = true)

        // Weighted duration is less than raw duration, so weighted speed is faster (higher number)
        assertTrue("Weighted speed should be higher (faster) than unweighted",
            weightedSpeed > unweightedSpeed)
    }

    @Test
    fun `calculateDurationSeconds basic calculation`() {
        // 240 frames at 240fps = 1 second
        val duration = calculator.calculateDurationSeconds(0, 239, fps = 240.0)
        assertEquals(1.0, duration, 0.001)
    }

    @Test
    fun `calculateDurationSeconds with slow motion`() {
        // 240 frames at 30fps playback from 240fps recording
        // Real time = 240/240 = 1 second
        // But playback says 240/30 = 8 seconds
        // Time scale = 30/240 = 1/8
        // Actual = (240/30) * (30/240) = 1 second
        val duration = calculator.calculateDurationSeconds(0, 239, fps = 30.0, recordingFps = 240.0)
        assertEquals(1.0, duration, 0.001)
    }

    @Test
    fun `compareWithExpected calculates error correctly`() {
        // Measured 1/55, expected 1/60
        // Error = ((55-60)/60) * 100 = -8.33%
        val error = calculator.compareWithExpected(55.0, 60.0)
        assertEquals(-8.333, error, 0.01)
    }

    @Test
    fun `compareWithExpected positive error when faster`() {
        // Measured 1/70, expected 1/60 (measured is faster)
        val error = calculator.compareWithExpected(70.0, 60.0)
        assertTrue("Positive error when measured is faster", error > 0)
    }

    @Test
    fun `formatShutterSpeed formats correctly`() {
        assertEquals("1/500", calculator.formatShutterSpeed(500.0))
        assertEquals("1/60", calculator.formatShutterSpeed(60.0))
        assertEquals("1/1", calculator.formatShutterSpeed(1.0))
    }

    @Test
    fun `groupShutterEvents matches by duration`() {
        // Create events with different durations
        val events = listOf(
            ShutterEvent(0, 3, List(4) { 100.0 }),     // 4 frames (fastest)
            ShutterEvent(10, 17, List(8) { 100.0 }),   // 8 frames (medium)
            ShutterEvent(20, 35, List(16) { 100.0 })   // 16 frames (slowest)
        )

        // Expected speeds: 1/500, 1/250, 1/125 (at 240fps, these correspond to shorter durations)
        val expectedSpeeds = listOf(60.0, 30.0, 15.0)  // 1/60, 1/30, 1/15

        val grouped = calculator.groupShutterEvents(events, expectedSpeeds, fps = 240.0)

        // Should have 3 groups
        assertEquals(3, grouped.size)

        // Fastest event should match fastest expected speed
        assertTrue(grouped.containsKey(60.0))
        assertEquals(1, grouped[60.0]!!.size)
    }

    @Test
    fun `calculateAllSpeeds processes all events`() {
        val events = listOf(
            ShutterEvent(0, 3, List(4) { 100.0 }),
            ShutterEvent(10, 17, List(8) { 100.0 })
        )

        val results = calculator.calculateAllSpeeds(events, fps = 240.0)

        assertEquals(2, results.size)
        assertEquals(60.0, results[0].measuredSpeed, 0.001)  // 4 frames at 240fps
        assertEquals(30.0, results[1].measuredSpeed, 0.001)  // 8 frames at 240fps
        assertNull(results[0].expectedSpeed)
        assertNull(results[0].errorPercent)
    }
}
