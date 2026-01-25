package com.shutteranalyzer.ui.screens.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.analysis.ShutterSpeedCalculator
import com.shutteranalyzer.data.repository.CameraRepository
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.domain.model.Camera
import com.shutteranalyzer.domain.model.TestSession
import com.shutteranalyzer.ui.components.EventRegion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

/**
 * Represents a single shutter speed measurement result.
 */
data class ShutterResult(
    val expectedSpeed: String,
    val expectedMs: Double,
    val measuredMs: Double,
    val deviationPercent: Double
)

/**
 * Data for the brightness timeline chart.
 */
data class TimelineData(
    val brightnessValues: List<Double>,
    val events: List<EventRegion>,
    val threshold: Double,
    val baseline: Double
)

/**
 * ViewModel for the Results screen.
 */
@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val testSessionRepository: TestSessionRepository,
    private val cameraRepository: CameraRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    /**
     * The test session.
     */
    private val _session = MutableStateFlow<TestSession?>(null)
    val session: StateFlow<TestSession?> = _session.asStateFlow()

    /**
     * The camera (if associated).
     */
    private val _camera = MutableStateFlow<Camera?>(null)
    val camera: StateFlow<Camera?> = _camera.asStateFlow()

    /**
     * Calculated results.
     */
    private val _results = MutableStateFlow<List<ShutterResult>>(emptyList())
    val results: StateFlow<List<ShutterResult>> = _results.asStateFlow()

    /**
     * Average deviation across all measurements.
     */
    private val _averageDeviation = MutableStateFlow(0.0)
    val averageDeviation: StateFlow<Double> = _averageDeviation.asStateFlow()

    /**
     * Formatted test date.
     */
    private val _testDate = MutableStateFlow("")
    val testDate: StateFlow<String> = _testDate.asStateFlow()

    /**
     * Whether data has been saved.
     */
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    /**
     * Timeline chart data (brightness values, events, threshold).
     */
    private val _timelineData = MutableStateFlow<TimelineData?>(null)
    val timelineData: StateFlow<TimelineData?> = _timelineData.asStateFlow()

    init {
        loadSessionAndCalculate()
    }

    private fun loadSessionAndCalculate() {
        viewModelScope.launch {
            val loadedSession = testSessionRepository.getSessionById(sessionId)
            _session.value = loadedSession

            loadedSession?.let { session ->
                // Format test date
                _testDate.value = formatDate(session.testedAt)

                // Load camera if associated
                if (session.cameraId > 0) {
                    _camera.value = cameraRepository.getCameraById(session.cameraId)
                }

                // Calculate results from events
                calculateResults(session)

                // Build timeline data for brightness chart
                buildTimelineData(session)
            }
        }
    }

    /**
     * Build timeline data from session events for the brightness chart.
     */
    private fun buildTimelineData(session: TestSession) {
        if (session.events.isEmpty()) {
            _timelineData.value = null
            return
        }

        // Collect brightness values and build event regions
        val allBrightness = mutableListOf<Double>()
        val eventRegions = mutableListOf<EventRegion>()

        // Use stored expected speeds for labels
        val expectedSpeeds = session.expectedSpeeds

        // We need to reconstruct the timeline from individual events
        // Events are stored with their frame indices and brightness values
        var currentIndex = 0

        session.events.forEachIndexed { eventIndex, event ->
            // Add gap before event (simulate baseline frames)
            val gapFrames = 20 // Add some baseline frames between events
            if (eventIndex > 0) {
                val baselineValue = event.brightnessValues.minOrNull() ?: 30.0
                repeat(gapFrames) {
                    allBrightness.add(baselineValue + (Math.random() * 5 - 2.5))
                }
                currentIndex += gapFrames
            } else {
                // Add some baseline before first event
                val baselineValue = event.brightnessValues.minOrNull() ?: 30.0
                repeat(gapFrames) {
                    allBrightness.add(baselineValue + (Math.random() * 5 - 2.5))
                }
                currentIndex += gapFrames
            }

            // Record event start
            val eventStart = currentIndex

            // Add event brightness values
            event.brightnessValues.forEach { brightness ->
                allBrightness.add(brightness)
                currentIndex++
            }

            // Record event end
            val eventEnd = currentIndex

            // Get label for this event
            val label = expectedSpeeds.getOrNull(eventIndex) ?: ""

            eventRegions.add(EventRegion(eventStart, eventEnd, label))
        }

        // Add baseline after last event
        val lastEvent = session.events.lastOrNull()
        val finalBaseline = lastEvent?.brightnessValues?.minOrNull() ?: 30.0
        repeat(20) {
            allBrightness.add(finalBaseline + (Math.random() * 5 - 2.5))
        }

        // Calculate threshold and baseline from data
        val baseline = allBrightness.filter { it < 100 }.average().takeIf { it.isFinite() } ?: 30.0
        val peakAvg = session.events.flatMap { it.brightnessValues }.average().takeIf { it.isFinite() } ?: 150.0
        val threshold = baseline + (peakAvg - baseline) * 0.3

        _timelineData.value = TimelineData(
            brightnessValues = allBrightness,
            events = eventRegions,
            threshold = threshold,
            baseline = baseline
        )
    }

    private fun calculateResults(session: TestSession) {
        val calculatedResults = mutableListOf<ShutterResult>()

        // Use stored expected speeds from session
        val expectedSpeeds = session.expectedSpeeds

        session.events.forEachIndexed { index, event ->
            // Calculate measured duration
            val measuredSpeed = ShutterSpeedCalculator.calculateShutterSpeed(
                durationFrames = event.weightedDurationFrames,
                fps = session.recordingFps
            )
            val measuredMs = (1.0 / measuredSpeed) * 1000

            // Use stored expected speed, fall back to standard set only if not available
            val expectedSpeed = expectedSpeeds.getOrNull(index)
                ?: fallbackExpectedSpeed(index, session.events.size)
            val expectedMs = parseSpeedToMs(expectedSpeed)

            val deviation = if (expectedMs > 0) {
                ((measuredMs - expectedMs) / expectedMs) * 100
            } else 0.0

            calculatedResults.add(
                ShutterResult(
                    expectedSpeed = expectedSpeed,
                    expectedMs = expectedMs,
                    measuredMs = measuredMs,
                    deviationPercent = deviation
                )
            )
        }

        _results.value = calculatedResults

        // Calculate average deviation
        val avgDev = if (calculatedResults.isNotEmpty()) {
            calculatedResults.map { abs(it.deviationPercent) }.average()
        } else 0.0
        _averageDeviation.value = avgDev

        // Update session with average deviation
        viewModelScope.launch {
            testSessionRepository.updateAvgDeviation(sessionId, avgDev)
        }
    }

    /**
     * Fallback speed estimation for legacy sessions without stored expected speeds.
     */
    private fun fallbackExpectedSpeed(index: Int, total: Int): String {
        val speeds = listOf(
            "1/1000", "1/500", "1/250", "1/125", "1/60",
            "1/30", "1/15", "1/8", "1/4", "1/2", "1s"
        )
        return if (index < speeds.size) speeds[index] else speeds.lastOrNull() ?: "1/60"
    }

    private fun parseSpeedToMs(speed: String): Double {
        return when {
            speed == "1s" -> 1000.0
            speed == "2s" -> 2000.0
            speed == "4s" -> 4000.0
            speed.startsWith("1/") -> {
                val denominator = speed.removePrefix("1/").toDoubleOrNull() ?: 1.0
                1000.0 / denominator
            }
            else -> 1000.0 / (speed.toDoubleOrNull() ?: 1.0)
        }
    }

    private fun formatDate(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }

    /**
     * Save the session (marks as complete).
     */
    fun saveSession() {
        viewModelScope.launch {
            // The session is already saved, just mark as saved for UI
            _isSaved.value = true
        }
    }

    /**
     * Format milliseconds for display.
     */
    fun formatMs(ms: Double): String {
        return when {
            ms >= 1000 -> String.format("%.1fs", ms / 1000)
            ms >= 10 -> String.format("%.1fms", ms)
            else -> String.format("%.2fms", ms)
        }
    }

    /**
     * Format deviation for display.
     */
    fun formatDeviation(percent: Double): String {
        val sign = if (percent >= 0) "+" else ""
        return "$sign${String.format("%.0f", percent)}%"
    }
}
