package com.shutteranalyzer.ui.screens.recording

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.analysis.ShutterSpeedCalculator
import com.shutteranalyzer.data.camera.CameraState
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

private const val TAG = "RecordingViewModel"

/**
 * Recording state.
 */
sealed class RecordingState {
    object Initializing : RecordingState()
    /** Camera is running, user is setting up before starting detection */
    object SettingUp : RecordingState()
    /** Phase 1: Collecting baseline brightness frames */
    object CalibratingBaseline : RecordingState()
    /** Phase 2: Waiting for user to fire calibration shutter */
    object WaitingForCalibrationShutter : RecordingState()
    data class WaitingForShutter(val speed: String, val index: Int, val total: Int) : RecordingState()
    data class EventDetected(val speed: String, val index: Int) : RecordingState()
    /** Analyzing the recorded video to get accurate frame indices */
    data class Analyzing(val progress: Float) : RecordingState()
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
     * Whether waiting for calibration shutter (Phase 2 of calibration).
     */
    val isWaitingForCalibrationShutter: StateFlow<Boolean> = cameraManager.isWaitingForCalibrationShutter

    /**
     * Whether calibration is complete (both phases).
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
     * Zoom controls.
     */
    val zoomRatio: StateFlow<Float> = cameraManager.zoomRatio
    val minZoomRatio: StateFlow<Float> = cameraManager.minZoomRatio
    val maxZoomRatio: StateFlow<Float> = cameraManager.maxZoomRatio

    fun setZoom(ratio: Float) {
        cameraManager.setZoom(ratio)
    }

    /**
     * Focus controls.
     * Note: Switching focus modes requires camera rebinding (brief flicker).
     */
    val isAutoFocus: StateFlow<Boolean> = cameraManager.isAutoFocus
    val focusDistance: StateFlow<Float> = cameraManager.focusDistance

    fun enableAutoFocus() {
        cameraManager.enableAutoFocus()
    }

    fun enableManualFocus() {
        cameraManager.enableManualFocus()
    }

    fun setManualFocus(distance: Float) {
        cameraManager.setManualFocus(distance)
    }

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
        // Phase 1 → Phase 2: Baseline complete, wait for calibration shutter
        viewModelScope.launch {
            cameraManager.isWaitingForCalibrationShutter.collect { waiting ->
                if (waiting && _recordingState.value is RecordingState.CalibratingBaseline) {
                    _recordingState.value = RecordingState.WaitingForCalibrationShutter
                }
            }
        }

        // Phase 2 complete: Full calibration done, start prompting for speeds
        viewModelScope.launch {
            cameraManager.isCalibrated.collect { calibrated ->
                if (calibrated && _recordingState.value is RecordingState.WaitingForCalibrationShutter) {
                    // Calibration shutter was fired and captured - start actual detection
                    startWaitingForFirstShutter()
                }
            }
        }
    }

    /** Track the number of events we've already processed to avoid reprocessing */
    private var lastProcessedEventCount = 0

    private fun observeEvents() {
        viewModelScope.launch {
            cameraManager.detectedEvents.collect { events ->
                // Only process new events when we're waiting for a shutter event
                val currentState = _recordingState.value
                if (currentState !is RecordingState.WaitingForShutter) {
                    return@collect
                }

                // Only process if there are new events since last processing
                if (events.size <= lastProcessedEventCount) {
                    return@collect
                }
                lastProcessedEventCount = events.size

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

    /**
     * Initialize the camera.
     */
    suspend fun initializeCamera(): Boolean {
        return cameraManager.initialize()
    }

    /**
     * Bind camera preview to the lifecycle.
     */
    fun bindCameraPreview(lifecycleOwner: androidx.lifecycle.LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        cameraManager.bindPreview(lifecycleOwner, previewView)
    }

    /**
     * Set the expected speeds to test.
     */
    fun setExpectedSpeeds(speeds: List<String>) {
        _expectedSpeeds.value = speeds
    }

    /**
     * Start video recording (user can set up before detection begins).
     */
    fun startRecording() {
        _recordingState.value = RecordingState.SettingUp

        cameraManager.startRecording(
            onComplete = { uri ->
                recordedVideoUri = uri
                Log.d(TAG, "Recording complete, URI: $uri")
                // Save live-detected events directly (frame indices are now correct)
                viewModelScope.launch {
                    saveLiveDetectedEvents(uri)
                    _recordingState.value = RecordingState.Complete
                }
            },
            onError = { error ->
                _recordingState.value = RecordingState.Error(error)
            }
        )
    }

    /**
     * Begin the detection/calibration process (called after user is ready).
     * Starts two-phase calibration:
     * 1. CalibratingBaseline: Collect dark frames to establish baseline
     * 2. WaitingForCalibrationShutter: User fires shutter once to calibrate peak
     */
    fun beginDetection() {
        _recordingState.value = RecordingState.CalibratingBaseline
        _currentSpeedIndex.value = 0
        lastProcessedEventCount = 0
        cameraManager.resetCalibration()
    }

    /**
     * Stop recording. Events are saved in onComplete callback.
     */
    fun stopRecording() {
        Log.d(TAG, "Stopping recording...")
        cameraManager.stopRecording()
    }

    /**
     * Save events from live detection.
     */
    private suspend fun saveLiveDetectedEvents(videoUri: Uri) {
        val markers = cameraManager.getDetectedEvents()
        if (markers.isEmpty()) {
            // Just save the video URI even if no events
            testSessionRepository.updateVideoUri(sessionId, videoUri.toString())
            return
        }

        // Convert EventMarkers (timestamps) to ShutterEvents (frame indices)
        val events = com.shutteranalyzer.data.camera.EventConverter.toShutterEvents(
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

        // Save video URI
        testSessionRepository.updateVideoUri(sessionId, videoUri.toString())
        Log.d(TAG, "Saved ${events.size} live-detected events")
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
