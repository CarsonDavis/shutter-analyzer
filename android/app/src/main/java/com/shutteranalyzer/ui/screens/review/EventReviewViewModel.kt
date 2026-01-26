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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "EventReviewViewModel"

/**
 * Manual state override for frame classification.
 * Allows users to override the automatic brightness-based classification.
 */
enum class FrameState {
    FULL,      // Fully open shutter - weight = 1.0, included
    PARTIAL,   // Transitioning shutter - weight = 0.5, included
    EXCLUDED   // Excluded from calculation - not included
}

/**
 * Represents a frame in an event for display.
 */
data class FrameInfo(
    val frameNumber: Int,
    val brightness: Double,
    val isIncluded: Boolean,
    val weight: Double,  // 0.0-1.0, where 1.0 is full brightness
    val isContext: Boolean = false,  // True for before/after context frames
    val thumbnail: Bitmap? = null,  // Actual frame thumbnail from video
    val manualState: FrameState? = null  // User override, null means use automatic classification
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
     * Frame state overrides (mutable by user).
     * Key is (eventIndex, frameIndex), value is the manual state.
     */
    private val frameStateOverrides = mutableMapOf<Pair<Int, Int>, FrameState>()

    /**
     * Track which events have thumbnails loaded.
     */
    private val loadedEventIndices = mutableSetOf<Int>()

    /**
     * Track extra context frames added by user before each event.
     * Key is eventIndex, value is the number of extra frames added.
     */
    private val extraFramesBefore = mutableMapOf<Int, Int>()

    /**
     * Track extra context frames added by user after each event.
     * Key is eventIndex, value is the number of extra frames added.
     */
    private val extraFramesAfter = mutableMapOf<Int, Int>()

    /**
     * Cache of thumbnails per event index.
     * Used for LRU eviction when memory limit exceeded.
     */
    private val eventThumbnailCache = mutableMapOf<Int, Map<Int, Bitmap>>()

    /**
     * Background prefetch job - cancelled when user navigates.
     */
    private var prefetchJob: Job? = null

    /**
     * Cached video URI and session for frame extraction.
     */
    private var cachedVideoUri: Uri? = null
    private var cachedSession: TestSession? = null

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

                // Cache session and video URI for lazy loading
                cachedSession = session
                cachedVideoUri = session.videoUri?.let { Uri.parse(it) }

                // Create initial review events without thumbnails
                val reviewEvents = session.events.mapIndexed { index, event ->
                    createReviewEvent(index, event, session.recordingFps, emptyMap())
                }
                _events.value = reviewEvents

                // Load thumbnails for event 0 ONLY (fast initial load)
                if (cachedVideoUri != null && session.events.isNotEmpty()) {
                    Log.d(TAG, "Loading thumbnails for event 0 only (lazy loading)")
                    loadThumbnailsForEvent(0)
                } else if (session.videoUri == null) {
                    Log.w(TAG, "No video URI available for session $sessionId")
                }
            } ?: run {
                Log.e(TAG, "Session not found: $sessionId")
            }
        }
    }

    /**
     * Load thumbnails for a specific event only.
     * This is the core of lazy loading - extracts frames for just one event.
     */
    private fun loadThumbnailsForEvent(eventIndex: Int) {
        val session = cachedSession ?: return
        val videoUri = cachedVideoUri ?: return

        if (eventIndex !in session.events.indices) return
        if (loadedEventIndices.contains(eventIndex)) {
            Log.d(TAG, "Event $eventIndex already loaded, skipping extraction")
            // Still prefetch adjacent events
            prefetchAdjacentEvents(eventIndex)
            return
        }

        Log.d(TAG, "Loading thumbnails for event $eventIndex")
        _isLoadingThumbnails.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val event = session.events[eventIndex]

            // Collect frame numbers for this single event
            val frameNumbers = mutableListOf<Int>()

            // Context frames before (dynamic count)
            val contextBefore = getContextFramesBefore(eventIndex)
            for (i in contextBefore downTo 1) {
                frameNumbers.add(event.startFrame - i)
            }

            // Event frames - sample if too many
            val eventFrameCount = event.brightnessValues.size
            if (eventFrameCount <= MAX_FRAMES_PER_EVENT) {
                for (i in 0 until eventFrameCount) {
                    frameNumbers.add(event.startFrame + i)
                }
            } else {
                val step = eventFrameCount.toDouble() / (MAX_FRAMES_PER_EVENT - 1)
                for (i in 0 until MAX_FRAMES_PER_EVENT) {
                    val frameIdx = (i * step).toInt().coerceAtMost(eventFrameCount - 1)
                    frameNumbers.add(event.startFrame + frameIdx)
                }
            }

            // Context frames after (dynamic count)
            val contextAfter = getContextFramesAfter(eventIndex)
            for (i in 1..contextAfter) {
                frameNumbers.add(event.endFrame + i)
            }

            Log.d(TAG, "Extracting ${frameNumbers.size} frames for event $eventIndex")

            val thumbnails = frameExtractor.extractFrames(
                videoUri = videoUri,
                frameNumbers = frameNumbers.filter { it >= 0 }.distinct(),
                fps = session.recordingFps,
                thumbnailWidth = 160
            )

            Log.d(TAG, "Extracted ${thumbnails.size} thumbnails for event $eventIndex")

            // Update state on main thread
            withContext(Dispatchers.Main) {
                // Cache thumbnails for this event
                eventThumbnailCache[eventIndex] = thumbnails
                loadedEventIndices.add(eventIndex)

                // Evict old events if over memory limit
                evictOldEvents(eventIndex)

                // Update just this event with thumbnails
                updateEventWithThumbnails(eventIndex, thumbnails)

                _isLoadingThumbnails.value = false

                // Start prefetching adjacent events
                prefetchAdjacentEvents(eventIndex)
            }
        }
    }

    /**
     * Update a single event with its thumbnails.
     */
    private fun updateEventWithThumbnails(eventIndex: Int, thumbnails: Map<Int, Bitmap>) {
        val session = cachedSession ?: return
        if (eventIndex !in session.events.indices) return

        val currentEvents = _events.value.toMutableList()
        if (eventIndex in currentEvents.indices) {
            val event = session.events[eventIndex]
            currentEvents[eventIndex] = createReviewEvent(
                eventIndex, event, session.recordingFps, thumbnails
            )
            _events.value = currentEvents
        }
    }

    /**
     * Prefetch adjacent events in background.
     * Prioritizes next event, then previous event.
     */
    private fun prefetchAdjacentEvents(currentIndex: Int) {
        val session = cachedSession ?: return

        // Cancel any existing prefetch job
        prefetchJob?.cancel()

        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            // Prefetch next event (priority)
            val nextIndex = currentIndex + 1
            if (nextIndex in session.events.indices && !loadedEventIndices.contains(nextIndex)) {
                Log.d(TAG, "Prefetching event $nextIndex")
                prefetchEvent(nextIndex)
            }

            // Prefetch previous event (lower priority)
            val prevIndex = currentIndex - 1
            if (prevIndex >= 0 && !loadedEventIndices.contains(prevIndex)) {
                Log.d(TAG, "Prefetching event $prevIndex")
                prefetchEvent(prevIndex)
            }
        }
    }

    /**
     * Prefetch a single event's thumbnails (called from background thread).
     */
    private suspend fun prefetchEvent(eventIndex: Int) {
        val session = cachedSession ?: return
        val videoUri = cachedVideoUri ?: return

        if (eventIndex !in session.events.indices) return
        if (loadedEventIndices.contains(eventIndex)) return

        val event = session.events[eventIndex]

        // Collect frame numbers
        val frameNumbers = mutableListOf<Int>()

        // Context frames before (dynamic count)
        val contextBefore = getContextFramesBefore(eventIndex)
        for (i in contextBefore downTo 1) {
            frameNumbers.add(event.startFrame - i)
        }

        val eventFrameCount = event.brightnessValues.size
        if (eventFrameCount <= MAX_FRAMES_PER_EVENT) {
            for (i in 0 until eventFrameCount) {
                frameNumbers.add(event.startFrame + i)
            }
        } else {
            val step = eventFrameCount.toDouble() / (MAX_FRAMES_PER_EVENT - 1)
            for (i in 0 until MAX_FRAMES_PER_EVENT) {
                val frameIdx = (i * step).toInt().coerceAtMost(eventFrameCount - 1)
                frameNumbers.add(event.startFrame + frameIdx)
            }
        }

        // Context frames after (dynamic count)
        val contextAfter = getContextFramesAfter(eventIndex)
        for (i in 1..contextAfter) {
            frameNumbers.add(event.endFrame + i)
        }

        val thumbnails = frameExtractor.extractFrames(
            videoUri = videoUri,
            frameNumbers = frameNumbers.filter { it >= 0 }.distinct(),
            fps = session.recordingFps,
            thumbnailWidth = 160
        )

        // Update on main thread
        withContext(Dispatchers.Main) {
            // Check again in case it was loaded while we were extracting
            if (!loadedEventIndices.contains(eventIndex)) {
                eventThumbnailCache[eventIndex] = thumbnails
                loadedEventIndices.add(eventIndex)
                evictOldEvents(eventIndex)
                updateEventWithThumbnails(eventIndex, thumbnails)
                Log.d(TAG, "Prefetch complete for event $eventIndex")
            }
        }
    }

    /**
     * Evict old events from memory when over the limit.
     * Keeps events closest to current index.
     */
    private fun evictOldEvents(currentIndex: Int) {
        if (loadedEventIndices.size <= MAX_EVENTS_IN_MEMORY) return

        // Sort loaded events by distance from current
        val sortedByDistance = loadedEventIndices
            .sortedBy { kotlin.math.abs(it - currentIndex) }

        // Keep closest MAX_EVENTS_IN_MEMORY, evict the rest
        val toKeep = sortedByDistance.take(MAX_EVENTS_IN_MEMORY).toSet()
        val toEvict = loadedEventIndices - toKeep

        for (idx in toEvict) {
            Log.d(TAG, "Evicting thumbnails for event $idx (too far from current $currentIndex)")
            // Recycle bitmaps
            eventThumbnailCache[idx]?.values?.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            eventThumbnailCache.remove(idx)
            loadedEventIndices.remove(idx)

            // Clear thumbnails from the event in state
            val session = cachedSession ?: continue
            if (idx in session.events.indices) {
                val currentEvents = _events.value.toMutableList()
                if (idx in currentEvents.indices) {
                    val event = session.events[idx]
                    currentEvents[idx] = createReviewEvent(idx, event, session.recordingFps, emptyMap())
                    _events.value = currentEvents
                }
            }
        }
    }

    companion object {
        /** Maximum frames to show per event in review screen */
        private const val MAX_FRAMES_PER_EVENT = 12

        /** Maximum events to keep thumbnails in memory (LRU eviction) */
        private const val MAX_EVENTS_IN_MEMORY = 5

        /** Default number of context frames shown on each side */
        private const val DEFAULT_CONTEXT_FRAMES = 1

        /** Number of frames to add when user clicks [+] expand button */
        private const val FRAMES_TO_ADD_ON_EXPAND = 3
    }

    /**
     * Get total context frames to show before an event.
     */
    fun getContextFramesBefore(eventIndex: Int): Int {
        return DEFAULT_CONTEXT_FRAMES + (extraFramesBefore[eventIndex] ?: 0)
    }

    /**
     * Get total context frames to show after an event.
     */
    fun getContextFramesAfter(eventIndex: Int): Int {
        return DEFAULT_CONTEXT_FRAMES + (extraFramesAfter[eventIndex] ?: 0)
    }

    /**
     * Add more frames before the current event.
     * Called when user taps the [+] button on the left.
     */
    fun addFramesBefore(eventIndex: Int) {
        val session = cachedSession ?: return
        if (eventIndex !in session.events.indices) return

        val event = session.events[eventIndex]
        val currentBefore = getContextFramesBefore(eventIndex)

        // Check if we can add more frames (don't go below frame 0)
        if (event.startFrame - currentBefore - FRAMES_TO_ADD_ON_EXPAND < 0) {
            // Calculate how many frames we can actually add
            val maxCanAdd = event.startFrame - currentBefore
            if (maxCanAdd > 0) {
                extraFramesBefore[eventIndex] = (extraFramesBefore[eventIndex] ?: 0) + maxCanAdd
            }
        } else {
            extraFramesBefore[eventIndex] = (extraFramesBefore[eventIndex] ?: 0) + FRAMES_TO_ADD_ON_EXPAND
        }

        Log.d(TAG, "Added frames before event $eventIndex, total before: ${getContextFramesBefore(eventIndex)}")

        // Mark as needing reload and reload thumbnails
        loadedEventIndices.remove(eventIndex)
        eventThumbnailCache.remove(eventIndex)
        loadThumbnailsForEvent(eventIndex)
    }

    /**
     * Add more frames after the current event.
     * Called when user taps the [+] button on the right.
     */
    fun addFramesAfter(eventIndex: Int) {
        val session = cachedSession ?: return
        if (eventIndex !in session.events.indices) return

        // Add frames (video end boundary is handled gracefully by frame extraction)
        extraFramesAfter[eventIndex] = (extraFramesAfter[eventIndex] ?: 0) + FRAMES_TO_ADD_ON_EXPAND

        Log.d(TAG, "Added frames after event $eventIndex, total after: ${getContextFramesAfter(eventIndex)}")

        // Mark as needing reload and reload thumbnails
        loadedEventIndices.remove(eventIndex)
        eventThumbnailCache.remove(eventIndex)
        loadThumbnailsForEvent(eventIndex)
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
            val autoWeight = if (range > 0) {
                ((brightness - baseline) / range).coerceIn(0.0, 1.0)
            } else {
                1.0
            }

            val key = index to frameIdx
            val manualState = frameStateOverrides[key]
            val frameNumber = event.startFrame + frameIdx

            // Apply manual state override if set
            val (weight, isIncluded) = when (manualState) {
                FrameState.FULL -> 1.0 to true
                FrameState.PARTIAL -> 0.5 to true
                FrameState.EXCLUDED -> 0.0 to false
                null -> autoWeight to true  // Use automatic classification
            }

            FrameInfo(
                frameNumber = frameNumber,
                brightness = brightness,
                isIncluded = isIncluded,
                weight = weight,
                isContext = false,
                thumbnail = thumbnails[frameNumber],
                manualState = manualState
            )
        }

        // Add context frames (dynamic count before and after) - also support manual state override
        val contextBeforeCount = getContextFramesBefore(index)
        val contextAfterCount = getContextFramesAfter(index)

        // Build context before indices: e.g., if contextBeforeCount=4, indices are -4, -3, -2, -1
        val contextBeforeIndices = (-contextBeforeCount until 0).toList()

        val contextBefore = contextBeforeIndices.mapIndexed { idx, offset ->
            val contextKey = index to (-(contextBeforeCount - idx))  // Use negative indices for context before
            val contextManualState = frameStateOverrides[contextKey]
            val frameNumber = event.startFrame + offset

            val (weight, isIncluded) = when (contextManualState) {
                FrameState.FULL -> 1.0 to true
                FrameState.PARTIAL -> 0.5 to true
                FrameState.EXCLUDED -> 0.0 to false
                null -> 0.0 to false  // Context frames excluded by default
            }

            FrameInfo(
                frameNumber = frameNumber,
                brightness = baseline,
                isIncluded = isIncluded,
                weight = weight,
                isContext = true,
                thumbnail = thumbnails[frameNumber],
                manualState = contextManualState
            )
        }

        val contextAfter = (0 until contextAfterCount).map { idx ->
            val contextKey = index to (1000 + idx)  // Use large indices for context after
            val contextManualState = frameStateOverrides[contextKey]
            val frameNumber = event.endFrame + 1 + idx

            val (weight, isIncluded) = when (contextManualState) {
                FrameState.FULL -> 1.0 to true
                FrameState.PARTIAL -> 0.5 to true
                FrameState.EXCLUDED -> 0.0 to false
                null -> 0.0 to false  // Context frames excluded by default
            }

            FrameInfo(
                frameNumber = frameNumber,
                brightness = baseline,
                isIncluded = isIncluded,
                weight = weight,
                isContext = true,
                thumbnail = thumbnails[frameNumber],
                manualState = contextManualState
            )
        }

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
            val newIndex = _currentEventIndex.value + 1
            _currentEventIndex.value = newIndex

            // Load thumbnails if not already loaded
            if (!loadedEventIndices.contains(newIndex)) {
                loadThumbnailsForEvent(newIndex)
            } else {
                // Already loaded, just trigger prefetch for adjacent events
                prefetchAdjacentEvents(newIndex)
            }
        }
    }

    /**
     * Navigate to the previous event.
     */
    fun previousEvent() {
        if (_currentEventIndex.value > 0) {
            val newIndex = _currentEventIndex.value - 1
            _currentEventIndex.value = newIndex

            // Load thumbnails if not already loaded
            if (!loadedEventIndices.contains(newIndex)) {
                loadThumbnailsForEvent(newIndex)
            } else {
                // Already loaded, just trigger prefetch for adjacent events
                prefetchAdjacentEvents(newIndex)
            }
        }
    }

    /**
     * Cycle frame state for the specified frame index.
     * Cycles through: FULL → PARTIAL → EXCLUDED → FULL (back to auto if context)
     *
     * For context frames (initially excluded), cycles: FULL → PARTIAL → EXCLUDED (back to default)
     * For event frames (initially auto), cycles based on current state.
     */
    fun cycleFrameState(frameIndex: Int) {
        val eventIndex = _currentEventIndex.value
        val currentEvents = _events.value.toMutableList()
        if (eventIndex !in currentEvents.indices) return

        val currentEvent = currentEvents[eventIndex]
        if (frameIndex !in currentEvent.frames.indices) return

        val frame = currentEvent.frames[frameIndex]

        // Determine the storage key based on whether it's a context frame
        // Context frames use special indices: negative for before, 1000+ for after
        val contextBeforeCount = getContextFramesBefore(eventIndex)
        val contextAfterCount = getContextFramesAfter(eventIndex)

        val storageKey = when {
            frame.isContext && frameIndex < contextBeforeCount -> eventIndex to (-(contextBeforeCount - frameIndex))  // Context before
            frame.isContext -> eventIndex to (1000 + (frameIndex - currentEvent.frames.size + contextAfterCount))  // Context after
            else -> eventIndex to (frameIndex - contextBeforeCount)  // Regular frames (offset by context frames before)
        }

        // Determine current state and next state
        val currentState = frame.manualState

        // Determine the automatic state for non-context frames
        val autoState = when {
            frame.isContext -> null  // Context frames have no auto state
            frame.weight >= 0.95 -> FrameState.FULL
            frame.weight >= 0.5 -> FrameState.PARTIAL
            else -> FrameState.EXCLUDED
        }

        // Cycle to next state
        val nextState = when (currentState) {
            null -> FrameState.FULL  // From auto/default -> FULL
            FrameState.FULL -> FrameState.PARTIAL
            FrameState.PARTIAL -> FrameState.EXCLUDED
            FrameState.EXCLUDED -> {
                // For context frames, cycle back to FULL
                // For regular frames, go back to auto (null) if different from FULL, else stay at FULL
                if (frame.isContext) {
                    FrameState.FULL
                } else if (autoState == FrameState.FULL) {
                    FrameState.PARTIAL  // Skip auto since it's the same as FULL
                } else {
                    null  // Return to auto
                }
            }
        }

        // Update the override map
        if (nextState == null) {
            frameStateOverrides.remove(storageKey)
        } else {
            frameStateOverrides[storageKey] = nextState
        }

        // Apply the new state to the frame
        val (newWeight, newIsIncluded) = when (nextState) {
            FrameState.FULL -> 1.0 to true
            FrameState.PARTIAL -> 0.5 to true
            FrameState.EXCLUDED -> 0.0 to false
            null -> {
                // Return to automatic classification
                if (frame.isContext) {
                    0.0 to false
                } else {
                    frame.weight to true  // Original auto weight
                }
            }
        }

        val updatedFrames = currentEvent.frames.mapIndexed { idx, f ->
            if (idx == frameIndex) {
                f.copy(
                    isIncluded = newIsIncluded,
                    weight = newWeight,
                    manualState = nextState
                )
            } else {
                f
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

    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        prefetchJob?.cancel()

        // Recycle all cached bitmaps
        eventThumbnailCache.values.forEach { thumbnails ->
            thumbnails.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
        eventThumbnailCache.clear()
        loadedEventIndices.clear()

        Log.d(TAG, "EventReviewViewModel cleared, bitmaps recycled")
    }
}
