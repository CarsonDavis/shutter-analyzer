package com.shutteranalyzer.ui.screens.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.analysis.ShutterSpeedCalculator
import com.shutteranalyzer.analysis.model.ShutterEvent
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.domain.model.TestSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents a frame in an event for display.
 */
data class FrameInfo(
    val frameNumber: Int,
    val brightness: Double,
    val isIncluded: Boolean,
    val weight: Double,  // 0.0-1.0, where 1.0 is full brightness
    val isContext: Boolean = false  // True for before/after context frames
)

/**
 * Represents an event for review.
 */
data class ReviewEvent(
    val index: Int,
    val expectedSpeed: String,
    val frames: List<FrameInfo>,
    val startFrame: Int,
    val endFrame: Int
)

/**
 * ViewModel for the Event Review screen.
 */
@HiltViewModel
class EventReviewViewModel @Inject constructor(
    private val testSessionRepository: TestSessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    /**
     * The test session being reviewed.
     */
    private val _session = MutableStateFlow<TestSession?>(null)
    val session: StateFlow<TestSession?> = _session.asStateFlow()

    /**
     * Events for review with frame data.
     */
    private val _events = MutableStateFlow<List<ReviewEvent>>(emptyList())
    val events: StateFlow<List<ReviewEvent>> = _events.asStateFlow()

    /**
     * Current event index being reviewed.
     */
    private val _currentEventIndex = MutableStateFlow(0)
    val currentEventIndex: StateFlow<Int> = _currentEventIndex.asStateFlow()

    /**
     * Frame inclusion state (mutable by user).
     */
    private val frameInclusions = mutableMapOf<Pair<Int, Int>, Boolean>()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val loadedSession = testSessionRepository.getSessionById(sessionId)
            _session.value = loadedSession

            loadedSession?.let { session ->
                val reviewEvents = session.events.mapIndexed { index, event ->
                    createReviewEvent(index, event, session.recordingFps)
                }
                _events.value = reviewEvents
            }
        }
    }

    private fun createReviewEvent(
        index: Int,
        event: ShutterEvent,
        fps: Double
    ): ReviewEvent {
        val baseline = event.baselineBrightness ?: event.brightnessValues.minOrNull() ?: 0.0
        val peak = event.peakBrightness ?: event.brightnessValues.maxOrNull() ?: 255.0
        val range = peak - baseline

        val frames = event.brightnessValues.mapIndexed { frameIdx, brightness ->
            val weight = if (range > 0) {
                ((brightness - baseline) / range).coerceIn(0.0, 1.0)
            } else {
                1.0
            }

            val key = index to frameIdx
            val isIncluded = frameInclusions[key] ?: true

            FrameInfo(
                frameNumber = event.startFrame + frameIdx,
                brightness = brightness,
                isIncluded = isIncluded,
                weight = weight,
                isContext = false
            )
        }

        // Add context frames (2 before and 2 after, simulated)
        val contextBefore = listOf(
            FrameInfo(event.startFrame - 2, baseline, false, 0.0, isContext = true),
            FrameInfo(event.startFrame - 1, baseline, false, 0.0, isContext = true)
        )
        val contextAfter = listOf(
            FrameInfo(event.endFrame + 1, baseline, false, 0.0, isContext = true),
            FrameInfo(event.endFrame + 2, baseline, false, 0.0, isContext = true)
        )

        // Calculate expected speed based on fps
        val durationSeconds = event.durationFrames / fps
        val expectedSpeed = ShutterSpeedCalculator.formatShutterSpeed(1.0 / durationSeconds)

        return ReviewEvent(
            index = index,
            expectedSpeed = expectedSpeed,
            frames = contextBefore + frames + contextAfter,
            startFrame = event.startFrame,
            endFrame = event.endFrame
        )
    }

    /**
     * Navigate to the next event.
     */
    fun nextEvent() {
        val events = _events.value
        if (_currentEventIndex.value < events.size - 1) {
            _currentEventIndex.value++
        }
    }

    /**
     * Navigate to the previous event.
     */
    fun previousEvent() {
        if (_currentEventIndex.value > 0) {
            _currentEventIndex.value--
        }
    }

    /**
     * Toggle frame inclusion for the current event.
     */
    fun toggleFrameInclusion(frameIndex: Int) {
        val eventIndex = _currentEventIndex.value
        val key = eventIndex to frameIndex
        frameInclusions[key] = !(frameInclusions[key] ?: true)

        // Refresh the events list
        val currentEvents = _events.value.toMutableList()
        val currentEvent = currentEvents[eventIndex]
        val updatedFrames = currentEvent.frames.mapIndexed { idx, frame ->
            if (idx == frameIndex && !frame.isContext) {
                frame.copy(isIncluded = frameInclusions[key] ?: true)
            } else {
                frame
            }
        }
        currentEvents[eventIndex] = currentEvent.copy(frames = updatedFrames)
        _events.value = currentEvents
    }

    /**
     * Confirm events and trigger calculation.
     */
    suspend fun confirmAndCalculate() {
        // The actual calculation will happen in the Results screen
        // Here we could save any frame exclusions to the database if needed
    }

    /**
     * Get the current review event.
     */
    fun getCurrentEvent(): ReviewEvent? {
        val events = _events.value
        val index = _currentEventIndex.value
        return if (index in events.indices) events[index] else null
    }
}
