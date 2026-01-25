package com.shutteranalyzer.ui.screens.recording

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.analysis.ShutterSpeedCalculator
import com.shutteranalyzer.data.camera.CameraState
import com.shutteranalyzer.data.camera.EventConverter
import com.shutteranalyzer.data.camera.EventMarker
import com.shutteranalyzer.data.camera.ShutterCameraManager
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.ui.screens.setup.STANDARD_SPEEDS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Recording state.
 */
sealed class RecordingState {
    object Initializing : RecordingState()
    object Calibrating : RecordingState()
    data class WaitingForShutter(val speed: String, val index: Int, val total: Int) : RecordingState()
    data class EventDetected(val speed: String, val index: Int) : RecordingState()
    object Complete : RecordingState()
    data class Error(val message: String) : RecordingState()
}

/**
 * ViewModel for the Recording screen.
 */
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val cameraManager: ShutterCameraManager,
    private val testSessionRepository: TestSessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    /**
     * Recording state.
     */
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Initializing)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    /**
     * Current brightness level.
     */
    val currentBrightness: StateFlow<Double> = cameraManager.currentBrightness

    /**
     * Calibration progress (0-1).
     */
    val calibrationProgress: StateFlow<Float> = cameraManager.calibrationProgress

    /**
     * Whether calibration is complete.
     */
    val isCalibrated: StateFlow<Boolean> = cameraManager.isCalibrated

    /**
     * Detected events.
     */
    val detectedEvents: StateFlow<List<EventMarker>> = cameraManager.detectedEvents

    /**
     * Camera state.
     */
    val cameraState: StateFlow<CameraState> = cameraManager.cameraState

    /**
     * Current speed index.
     */
    private val _currentSpeedIndex = MutableStateFlow(0)
    val currentSpeedIndex: StateFlow<Int> = _currentSpeedIndex.asStateFlow()

    /**
     * Expected speeds to test.
     */
    private val _expectedSpeeds = MutableStateFlow<List<String>>(STANDARD_SPEEDS)
    val expectedSpeeds: StateFlow<List<String>> = _expectedSpeeds.asStateFlow()

    /**
     * Recording FPS.
     */
    private val _recordingFps = MutableStateFlow(30)
    val recordingFps: StateFlow<Int> = _recordingFps.asStateFlow()

    /**
     * Recorded video URI.
     */
    private var recordedVideoUri: Uri? = null

    init {
        loadSessionData()
        observeCalibration()
        observeEvents()
    }

    /**
     * Load expected speeds and FPS from the session.
     */
    private fun loadSessionData() {
        viewModelScope.launch {
            testSessionRepository.getSessionById(sessionId)?.let { session ->
                if (session.expectedSpeeds.isNotEmpty()) {
                    _expectedSpeeds.value = session.expectedSpeeds
                }
                _recordingFps.value = session.recordingFps.toInt()
            }
        }
    }

    private fun observeCalibration() {
        viewModelScope.launch {
            cameraManager.isCalibrated.collect { calibrated ->
                if (calibrated && _recordingState.value is RecordingState.Calibrating) {
                    advanceToNextSpeed()
                }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            cameraManager.detectedEvents.collect { events ->
                if (events.isNotEmpty()) {
                    val lastEvent = events.last()
                    val currentIndex = _currentSpeedIndex.value
                    val speeds = _expectedSpeeds.value

                    if (currentIndex < speeds.size) {
                        _recordingState.value = RecordingState.EventDetected(
                            speed = speeds[currentIndex],
                            index = currentIndex
                        )

                        // Auto-advance after a brief delay
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(1000)
                            advanceToNextSpeed()
                        }
                    }
                }
            }
        }
    }

    /**
     * Initialize the camera.
     */
    suspend fun initializeCamera(): Boolean {
        return cameraManager.initialize()
    }

    /**
     * Set the expected speeds to test.
     */
    fun setExpectedSpeeds(speeds: List<String>) {
        _expectedSpeeds.value = speeds
    }

    /**
     * Start the recording and calibration process.
     */
    fun startRecording() {
        _recordingState.value = RecordingState.Calibrating
        cameraManager.resetCalibration()

        cameraManager.startRecording(
            onComplete = { uri ->
                recordedVideoUri = uri
            },
            onError = { error ->
                _recordingState.value = RecordingState.Error(error)
            }
        )
    }

    /**
     * Stop recording and save detected events.
     */
    fun stopRecording() {
        cameraManager.stopRecording()

        // Check if any events were detected
        val events = cameraManager.getDetectedEvents()
        if (events.isEmpty()) {
            // Still complete, but with zero events - the UI will handle this
            saveDetectedEvents()
            _recordingState.value = RecordingState.Complete
        } else {
            saveDetectedEvents()
            _recordingState.value = RecordingState.Complete
        }
    }

    /**
     * Save detected events to the database with proper conversion.
     */
    private fun saveDetectedEvents() {
        viewModelScope.launch {
            val markers = cameraManager.getDetectedEvents()
            if (markers.isEmpty()) return@launch

            // Convert EventMarkers (timestamps) to ShutterEvents (frame indices)
            val events = EventConverter.toShutterEvents(
                markers = markers,
                recordingStartTimestamp = cameraManager.getRecordingStartTimestamp(),
                fps = _recordingFps.value.toDouble(),
                baselineBrightness = cameraManager.getBaselineBrightness()
            )

            // Calculate measured shutter speeds
            val measuredSpeeds = events.map { event ->
                ShutterSpeedCalculator.calculateShutterSpeed(
                    durationFrames = event.weightedDurationFrames,
                    fps = _recordingFps.value.toDouble()
                )
            }

            // Save events with expected speeds
            testSessionRepository.saveEventsWithExpectedSpeeds(
                sessionId = sessionId,
                events = events,
                measuredSpeeds = measuredSpeeds,
                expectedSpeeds = _expectedSpeeds.value
            )

            // Save video URI if we have one
            recordedVideoUri?.let { uri ->
                testSessionRepository.updateVideoUri(sessionId, uri.toString())
            }
        }
    }

    /**
     * Skip the current speed.
     */
    fun skipSpeed() {
        advanceToNextSpeed()
    }

    /**
     * Redo the last event (go back one speed).
     */
    fun redoLastEvent() {
        if (_currentSpeedIndex.value > 0) {
            _currentSpeedIndex.value--
            val speeds = _expectedSpeeds.value
            _recordingState.value = RecordingState.WaitingForShutter(
                speed = speeds[_currentSpeedIndex.value],
                index = _currentSpeedIndex.value,
                total = speeds.size
            )
        }
    }

    /**
     * Finish recording early.
     */
    fun finishEarly() {
        stopRecording()
    }

    private fun advanceToNextSpeed() {
        val speeds = _expectedSpeeds.value
        val nextIndex = _currentSpeedIndex.value + 1

        if (nextIndex >= speeds.size) {
            // All speeds completed
            stopRecording()
        } else {
            _currentSpeedIndex.value = nextIndex
            _recordingState.value = RecordingState.WaitingForShutter(
                speed = speeds[nextIndex],
                index = nextIndex,
                total = speeds.size
            )
        }
    }

    /**
     * Start waiting for the first shutter event.
     */
    private fun startWaitingForFirstShutter() {
        val speeds = _expectedSpeeds.value
        if (speeds.isNotEmpty()) {
            _recordingState.value = RecordingState.WaitingForShutter(
                speed = speeds[0],
                index = 0,
                total = speeds.size
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.stopRecording()
    }
}
