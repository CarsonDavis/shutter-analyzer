package com.shutteranalyzer.ui.screens.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.analysis.ShutterSpeedCalculator
import com.shutteranalyzer.data.repository.CameraRepository
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.domain.model.Camera
import com.shutteranalyzer.domain.model.TestSession
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
            }
        }
    }

    private fun calculateResults(session: TestSession) {
        val calculatedResults = mutableListOf<ShutterResult>()

        session.events.forEachIndexed { index, event ->
            // Calculate measured duration
            val measuredSpeed = ShutterSpeedCalculator.calculateShutterSpeed(
                durationFrames = event.weightedDurationFrames,
                fps = session.recordingFps
            )
            val measuredMs = (1.0 / measuredSpeed) * 1000

            // For demo purposes, we'll estimate expected speeds
            // In a real implementation, these would be stored with the events
            val expectedSpeed = estimateExpectedSpeed(index, session.events.size)
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

    private fun estimateExpectedSpeed(index: Int, total: Int): String {
        // Standard speed set
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
