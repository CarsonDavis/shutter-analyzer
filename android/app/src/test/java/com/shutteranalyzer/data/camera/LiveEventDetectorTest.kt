package com.shutteranalyzer.data.camera

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for LiveEventDetector state machine.
 */
class LiveEventDetectorTest {

    private lateinit var detector: LiveEventDetector

    @Before
    fun setUp() {
        detector = LiveEventDetector()
        detector.calibrationFrameCount = 10  // Shorter for testing
    }

    @Test
    fun `starts in calibrating state`() {
        assertTrue(detector.isCalibrating)
        assertFalse(detector.isCalibrated)
        assertEquals(0f, detector.calibrationProgress, 0.001f)
    }

    @Test
    fun `calibration progress updates correctly`() {
        repeat(5) { i ->
            detector.processFrame(20.0, i.toLong())
        }
        assertEquals(0.5f, detector.calibrationProgress, 0.001f)
    }

    @Test
    fun `calibration completes after enough frames`() {
        // Feed calibration frames (low brightness)
        repeat(9) { i ->
            val result = detector.processFrame(20.0, i.toLong())
            assertNull(result)
        }

        // Last calibration frame should trigger CalibrationComplete
        val result = detector.processFrame(20.0, 9)
        assertTrue(result is EventResult.CalibrationComplete)
        assertTrue(detector.isCalibrated)
        assertFalse(detector.isCalibrating)
    }

    @Test
    fun `threshold calculated correctly from calibration`() {
        // Mix of values: some low, some medium
        // 25th percentile should be baseline
        val values = listOf(10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0)
        values.forEachIndexed { i, v ->
            detector.processFrame(v, i.toLong())
        }

        // After calibration, threshold should be set
        assertTrue(detector.currentThreshold > detector.currentBaseline)
    }

    @Test
    fun `detects event when brightness exceeds threshold`() {
        // Calibrate with low brightness
        repeat(10) { i ->
            detector.processFrame(20.0, i.toLong())
        }

        // Verify calibrated
        assertTrue(detector.isCalibrated)

        // Now exceed threshold
        val result1 = detector.processFrame(200.0, 100)
        assertNull(result1)  // Event started but not finished
        assertTrue(detector.isEventInProgress)

        // Event continues
        val result2 = detector.processFrame(200.0, 101)
        assertNull(result2)

        // Event ends (brightness drops)
        val result3 = detector.processFrame(20.0, 102)
        assertTrue(result3 is EventResult.EventDetected)

        val event = result3 as EventResult.EventDetected
        assertEquals(100L, event.startTimestamp)
        assertEquals(102L, event.endTimestamp)
        assertEquals(2, event.brightnessValues.size)
    }

    @Test
    fun `counts detected events`() {
        // Calibrate
        repeat(10) { i ->
            detector.processFrame(20.0, i.toLong())
        }

        assertEquals(0, detector.detectedEventCount)

        // First event
        detector.processFrame(200.0, 100)
        detector.processFrame(20.0, 101)
        assertEquals(1, detector.detectedEventCount)

        // Second event
        detector.processFrame(200.0, 200)
        detector.processFrame(20.0, 201)
        assertEquals(2, detector.detectedEventCount)
    }

    @Test
    fun `reset clears all state`() {
        // Calibrate
        repeat(10) { i ->
            detector.processFrame(20.0, i.toLong())
        }

        // Detect event
        detector.processFrame(200.0, 100)
        detector.processFrame(20.0, 101)

        // Reset
        detector.reset()

        assertTrue(detector.isCalibrating)
        assertFalse(detector.isCalibrated)
        assertEquals(0, detector.detectedEventCount)
        assertEquals(0f, detector.calibrationProgress, 0.001f)
    }

    @Test
    fun `resetEvents keeps calibration`() {
        // Calibrate
        repeat(10) { i ->
            detector.processFrame(20.0, i.toLong())
        }

        val threshold = detector.currentThreshold

        // Detect event
        detector.processFrame(200.0, 100)
        detector.processFrame(20.0, 101)
        assertEquals(1, detector.detectedEventCount)

        // Reset events only
        detector.resetEvents()

        assertTrue(detector.isCalibrated)
        assertEquals(0, detector.detectedEventCount)
        assertEquals(threshold, detector.currentThreshold, 0.001)
    }

    @Test
    fun `handles uniform brightness during calibration`() {
        // All same brightness - should not crash
        repeat(10) { i ->
            detector.processFrame(50.0, i.toLong())
        }

        // Threshold should still be set to something reasonable
        assertTrue(detector.currentThreshold >= detector.currentBaseline)
    }

    @Test
    fun `collects brightness values during event`() {
        // Calibrate
        repeat(10) { i ->
            detector.processFrame(20.0, i.toLong())
        }

        // Event with varying brightness
        detector.processFrame(150.0, 100)
        detector.processFrame(180.0, 101)
        detector.processFrame(200.0, 102)
        detector.processFrame(180.0, 103)
        val result = detector.processFrame(20.0, 104)

        assertTrue(result is EventResult.EventDetected)
        val event = result as EventResult.EventDetected
        assertEquals(4, event.brightnessValues.size)
        assertEquals(listOf(150.0, 180.0, 200.0, 180.0), event.brightnessValues)
    }
}
