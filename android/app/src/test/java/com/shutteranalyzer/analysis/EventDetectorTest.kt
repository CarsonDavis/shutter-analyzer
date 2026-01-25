package com.shutteranalyzer.analysis

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for EventDetector.
 */
class EventDetectorTest {

    private lateinit var detector: EventDetector
    private lateinit var thresholdCalculator: ThresholdCalculator

    @Before
    fun setUp() {
        detector = EventDetector()
        thresholdCalculator = ThresholdCalculator()
    }

    @Test
    fun `findShutterEvents detects correct number of events`() {
        val stats = thresholdCalculator.analyzeBrightnessDistribution(TestVectors.sampleBrightnessValues)
        val events = detector.findShutterEvents(TestVectors.sampleBrightnessValues, stats.threshold)

        assertEquals("Should detect 3 shutter events", 3, events.size)
    }

    @Test
    fun `events have correct frame ranges`() {
        // Use a known threshold that will detect our test events
        val threshold = 50.0
        val events = detector.findShutterEvents(TestVectors.sampleBrightnessValues, threshold)

        // Events should be non-overlapping and in order
        for (i in 0 until events.size - 1) {
            assertTrue("Events should not overlap",
                events[i].endFrame < events[i + 1].startFrame)
        }
    }

    @Test
    fun `event brightness values are collected correctly`() {
        val threshold = 50.0
        val events = detector.findShutterEvents(TestVectors.sampleBrightnessValues, threshold)

        for (event in events) {
            // Each brightness value should be above threshold
            event.brightnessValues.forEach { brightness ->
                assertTrue("Event brightness should be above threshold",
                    brightness > threshold)
            }

            // Number of brightness values should match frame count
            val expectedFrameCount = event.endFrame - event.startFrame + 1
            assertEquals("Brightness values count should match frame count",
                expectedFrameCount, event.brightnessValues.size)
        }
    }

    @Test
    fun `no events detected when all frames below threshold`() {
        val lowBrightness = List(50) { 20.0 }
        val events = detector.findShutterEvents(lowBrightness, 50.0)

        assertEquals("Should detect no events", 0, events.size)
    }

    @Test
    fun `single event detected`() {
        val values = listOf(20.0, 20.0, 80.0, 100.0, 80.0, 20.0, 20.0)
        val events = detector.findShutterEvents(values, 50.0)

        assertEquals("Should detect 1 event", 1, events.size)
        assertEquals(2, events[0].startFrame)
        assertEquals(4, events[0].endFrame)
    }

    @Test
    fun `event at end of video is captured`() {
        val values = listOf(20.0, 20.0, 80.0, 100.0, 100.0)  // Ends while "open"
        val events = detector.findShutterEvents(values, 50.0)

        assertEquals("Should detect 1 event", 1, events.size)
        assertEquals(2, events[0].startFrame)
        assertEquals(4, events[0].endFrame)  // Last frame
    }

    @Test
    fun `calculatePeakBrightness returns reasonable value`() {
        val stats = thresholdCalculator.analyzeBrightnessDistribution(TestVectors.sampleBrightnessValues)
        val rawEvents = detector.findShutterEvents(TestVectors.sampleBrightnessValues, stats.threshold)
        val peak = detector.calculatePeakBrightness(rawEvents)

        assertNotNull(peak)
        assertTrue("Peak should be near 180", peak!! > 150 && peak < 200)
    }

    @Test
    fun `calculatePeakBrightness returns null for empty events`() {
        val peak = detector.calculatePeakBrightness(emptyList())
        assertNull(peak)
    }

    @Test
    fun `createShutterEvents sets baseline correctly`() {
        val rawEvents = listOf(
            RawShutterEvent(0, 4, listOf(60.0, 100.0, 100.0, 100.0, 60.0))
        )
        val shutterEvents = detector.createShutterEvents(rawEvents, baselineBrightness = 20.0)

        assertEquals(1, shutterEvents.size)
        assertEquals(20.0, shutterEvents[0].baselineBrightness!!, 0.001)
    }
}
