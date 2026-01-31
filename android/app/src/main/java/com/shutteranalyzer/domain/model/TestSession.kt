package com.shutteranalyzer.domain.model

import com.shutteranalyzer.analysis.model.ShutterEvent
import java.time.Instant

/**
 * Domain model representing a test session.
 *
 * Contains the test metadata and all shutter events from the test.
 */
data class TestSession(
    val id: Long = 0,
    val cameraId: Long? = null,
    val recordingFps: Double,
    val testedAt: Instant,
    val avgDeviationPercent: Double?,
    val events: List<ShutterEvent> = emptyList(),
    val expectedSpeeds: List<String> = emptyList(),
    val videoUri: String? = null,
    /** Cached event count for list display (avoids loading all events) */
    val eventCount: Int = events.size
)
