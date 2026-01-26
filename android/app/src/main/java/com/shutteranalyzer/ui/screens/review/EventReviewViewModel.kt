package com.shutteranalyzer.ui.screens.review

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.analysis.ShutterSpeedCalculator
import com.shutteranalyzer.analysis.model.ShutterEvent
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.data.video.FrameExtractor
import com.shutteranalyzer.domain.model.TestSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "EventReviewViewModel"

/**
 * Represents a frame in an event for display.
 */
data class FrameInfo(
    val frameNumber: Int,
    val brightness: Double,
    val isIncluded: Boolean,
    val weight: Double,  // 0.0-1.0, where 1.0 is full brightness
    val isContext: Boolean = false,  // True for before/after context frames
    val thumbnail: Bitmap? = null  // Actual frame thumbnail from video
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
    private val frameExtractor: FrameExtractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    /**
     * Loading state for thumbnail extraction.
     */
    private val _isLoadingThumbnails = MutableStateFlow(false)
    val isLoadingThumbnails: StateFlow<Boolean> = _isLoadingThumbnails.asStateFlow()

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
            Log.d(TAG, "Loading session: $sessionId")
            val loadedSession = testSessionRepository.getSessionById(sessionId)
            _session.value = loadedSession

            loadedSession?.let { session ->
                Log.d(TAG, "Session loaded: events=${session.events.size}, videoUri=${session.videoUri}")

                // Create initial review events without thumbnails
                val reviewEvents = session.events.mapIndexed { index, event ->
                    createReviewEvent(index, event, session.recordingFps, emptyMap())
                }
                _events.value = reviewEvents

                // Extract thumbnails in background if video URI is available
                if (session.videoUri != null) {
                    Log.d(TAG, "Extracting thumbnails from: ${session.videoUri}")
                    extractThumbnails(Uri.parse(session.videoUri), session)
                } else {
                    Log.w(TAG, "No video URI available for session $sessionId")
                }
            } ?: run {
                Log.e(TAG, "Session not found: $sessionId")
            }
        }
    }

    /**
     * Extract thumbnails for event frames from the video.
     * Limits extraction to key frames to avoid performance issues.
     */
    private suspend fun extractThumbnails(videoUri: Uri, session: TestSession) {
        _isLoadingThumbnails.value = true

        withContext(Dispatchers.IO) {
            // Collect frame numbers we need - limit per event to avoid extracting thousands
            val allFrameNumbers = mutableListOf<Int>()

            session.events.forEach { event ->
                // Context frames before (2)
                allFrameNumbers.add(event.startFrame - 2)
                allFrameNumbers.add(event.startFrame - 1)

                // Event frames - sample if too many
                // Use brightnessValues.size for consistency with createReviewEvent
                val eventFrameCount = event.brightnessValues.size
                if (eventFrameCount <= MAX_FRAMES_PER_EVENT) {
                    // Show all frames
                    for (i in 0 until eventFrameCount) {
                        allFrameNumbers.add(event.startFrame + i)
                    }
                } else {
                    // Sample frames: evenly spaced
                    val step = eventFrameCount.toDouble() / (MAX_FRAMES_PER_EVENT - 1)
                    for (i in 0 until MAX_FRAMES_PER_EVENT) {
                        val frameIdx = (i * step).toInt().coerceAtMost(eventFrameCount - 1)
                        allFrameNumbers.add(event.startFrame + frameIdx)
                    }
                }

                // Context frames after (2)
                allFrameNumbers.add(event.endFrame + 1)
                allFrameNumbers.add(event.endFrame + 2)
            }

            Log.d(TAG, "Extracting ${allFrameNumbers.size} frames for ${session.events.size} events")

            // Extract thumbnails
            val thumbnails = frameExtractor.extractFrames(
                videoUri = videoUri,
                frameNumbers = allFrameNumbers.filter { it >= 0 }.distinct(),
                fps = session.recordingFps,
                thumbnailWidth = 160
            )

            Log.d(TAG, "Extracted ${thumbnails.size} thumbnails")

            // Update events with thumbnails
            withContext(Dispatchers.Main) {
                val updatedEvents = session.events.mapIndexed { index, event ->
                    createReviewEvent(index, event, session.recordingFps, thumbnails)
                }
                _events.value = updatedEvents
                _isLoadingThumbnails.value = false
            }
        }
    }

    companion object {
        /** Maximum frames to show per event in review screen */
        private const val MAX_FRAMES_PER_EVENT = 12
    }

    private fun createReviewEvent(
        index: Int,
        event: ShutterEvent,
        fps: Double,
        thumbnails: Map<Int, Bitmap>
    ): ReviewEvent {
        val baseline = event.baselineBrightness ?: event.brightnessValues.minOrNull() ?: 0.0
        val peak = event.peakBrightness ?: event.brightnessValues.maxOrNull() ?: 255.0
        val range = peak - baseline

        // Sample frames if there are too many
        val eventFrameCount = event.brightnessValues.size
        val frameIndices = if (eventFrameCount <= MAX_FRAMES_PER_EVENT) {
            // Show all frames
            (0 until eventFrameCount).toList()
        } else {
            // Sample frames: evenly spaced
            val step = eventFrameCount.toDouble() / (MAX_FRAMES_PER_EVENT - 1)
            (0 until MAX_FRAMES_PER_EVENT).map { i ->
                (i * step).toInt().coerceAtMost(eventFrameCount - 1)
            }.distinct()
        }

        val frames = frameIndices.map { frameIdx ->
            val brightness = event.brightnessValues[frameIdx]
            val weight = if (range > 0) {
                ((brightness - baseline) / range).coerceIn(0.0, 1.0)
            } else {
                1.0
            }

            val key = index to frameIdx
            val isIncluded = frameInclusions[key] ?: true
            val frameNumber = event.startFrame + frameIdx

            FrameInfo(
                frameNumber = frameNumber,
                brightness = brightness,
                isIncluded = isIncluded,
                weight = weight,
                isContext = false,
                thumbnail = thumbnails[frameNumber]
            )
        }

        // Add context frames (2 before and 2 after)
        val contextBefore = listOf(
            FrameInfo(
                frameNumber = event.startFrame - 2,
                brightness = baseline,
                isIncluded = false,
                weight = 0.0,
                isContext = true,
                thumbnail = thumbnails[event.startFrame - 2]
            ),
            FrameInfo(
                frameNumber = event.startFrame - 1,
                brightness = baseline,
                isIncluded = false,
                weight = 0.0,
                isContext = true,
                thumbnail = thumbnails[event.startFrame - 1]
            )
        )
        val contextAfter = listOf(
            FrameInfo(
                frameNumber = event.endFrame + 1,
                brightness = baseline,
                isIncluded = false,
                weight = 0.0,
                isContext = true,
                thumbnail = thumbnails[event.endFrame + 1]
            ),
            FrameInfo(
                frameNumber = event.endFrame + 2,
                brightness = baseline,
                isIncluded = false,
                weight = 0.0,
                isContext = true,
                thumbnail = thumbnails[event.endFrame + 2]
            )
        )

        // Use stored expected speed, or calculate from duration as fallback
        val expectedSpeed = event.expectedSpeed
            ?: ShutterSpeedCalculator.formatShutterSpeed(1.0 / (event.durationFrames / fps))

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
