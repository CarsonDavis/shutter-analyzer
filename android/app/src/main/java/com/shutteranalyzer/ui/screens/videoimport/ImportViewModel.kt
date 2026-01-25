package com.shutteranalyzer.ui.screens.videoimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.analysis.model.ShutterEvent
import com.shutteranalyzer.data.repository.CameraRepository
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.data.video.AnalysisResult
import com.shutteranalyzer.data.video.VideoAnalyzer
import com.shutteranalyzer.data.video.VideoInfo
import com.shutteranalyzer.domain.model.Camera
import com.shutteranalyzer.domain.model.TestSession
import com.shutteranalyzer.ui.screens.setup.STANDARD_SPEEDS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * State for the import flow.
 */
sealed class ImportState {
    object SelectFile : ImportState()
    object Loading : ImportState()
    data class VideoSelected(val videoInfo: VideoInfo) : ImportState()
    data class Analyzing(val progress: Float) : ImportState()
    data class AssignSpeeds(
        val events: List<ShutterEvent>,
        val analysisResult: AnalysisResult
    ) : ImportState()
    data class Complete(val sessionId: Long) : ImportState()
    data class Error(val message: String) : ImportState()
}

/**
 * ViewModel for the Import screen.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val videoAnalyzer: VideoAnalyzer,
    private val testSessionRepository: TestSessionRepository,
    private val cameraRepository: CameraRepository
) : ViewModel() {

    /**
     * Current import state.
     */
    private val _importState = MutableStateFlow<ImportState>(ImportState.SelectFile)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    /**
     * Selected video URI.
     */
    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    /**
     * Video information.
     */
    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    /**
     * Assigned speeds for each event (event index -> speed string).
     */
    private val _assignedSpeeds = MutableStateFlow<Map<Int, String>>(emptyMap())
    val assignedSpeeds: StateFlow<Map<Int, String>> = _assignedSpeeds.asStateFlow()

    /**
     * Camera name for the import.
     */
    private val _cameraName = MutableStateFlow("")
    val cameraName: StateFlow<String> = _cameraName.asStateFlow()

    /**
     * Custom frame rate override.
     */
    private val _customFrameRate = MutableStateFlow<Double?>(null)
    val customFrameRate: StateFlow<Double?> = _customFrameRate.asStateFlow()

    /**
     * Analysis result storage.
     */
    private var analysisResult: AnalysisResult? = null

    /**
     * Handle video selection.
     */
    fun selectVideo(uri: Uri) {
        _importState.value = ImportState.Loading
        _videoUri.value = uri

        viewModelScope.launch {
            val info = videoAnalyzer.getVideoInfo(uri)
            if (info != null) {
                _videoInfo.value = info
                _importState.value = ImportState.VideoSelected(info)
            } else {
                _importState.value = ImportState.Error("Unable to read video file")
            }
        }
    }

    /**
     * Update camera name.
     */
    fun updateCameraName(name: String) {
        _cameraName.value = name
    }

    /**
     * Set custom frame rate override.
     */
    fun setCustomFrameRate(fps: Double?) {
        _customFrameRate.value = fps
    }

    /**
     * Start video analysis.
     */
    fun analyzeVideo() {
        val uri = _videoUri.value ?: return
        val info = _videoInfo.value ?: return
        val fps = _customFrameRate.value ?: info.frameRate

        _importState.value = ImportState.Analyzing(0f)

        viewModelScope.launch {
            val result = videoAnalyzer.analyzeVideo(
                uri = uri,
                frameRate = fps,
                onProgress = { progress ->
                    _importState.value = ImportState.Analyzing(progress)
                }
            )

            if (result != null && result.events.isNotEmpty()) {
                analysisResult = result

                // Initialize speed assignments with default guesses
                val defaultSpeeds = result.events.indices.associate { index ->
                    index to (STANDARD_SPEEDS.getOrElse(index) { "1/60" })
                }
                _assignedSpeeds.value = defaultSpeeds

                _importState.value = ImportState.AssignSpeeds(
                    events = result.events,
                    analysisResult = result
                )
            } else if (result != null && result.events.isEmpty()) {
                _importState.value = ImportState.Error("No shutter events detected in video")
            } else {
                _importState.value = ImportState.Error("Video analysis failed")
            }
        }
    }

    /**
     * Assign a speed to an event.
     */
    fun assignSpeed(eventIndex: Int, speed: String) {
        _assignedSpeeds.value = _assignedSpeeds.value + (eventIndex to speed)
    }

    /**
     * Create a session with the imported events.
     *
     * This function launches a coroutine to create the session asynchronously.
     * When complete, the importState will be set to ImportState.Complete with the session ID.
     */
    fun createSession() {
        val result = analysisResult ?: run {
            _importState.value = ImportState.Error("No analysis result available")
            return
        }
        val info = _videoInfo.value ?: run {
            _importState.value = ImportState.Error("No video info available")
            return
        }
        val speeds = _assignedSpeeds.value
        val fps = _customFrameRate.value ?: info.frameRate

        viewModelScope.launch {
            try {
                // Create camera if name provided
                val cameraId = if (_cameraName.value.isNotBlank()) {
                    val camera = Camera(
                        name = _cameraName.value.trim(),
                        createdAt = Instant.now()
                    )
                    cameraRepository.saveCamera(camera)
                } else {
                    0L
                }

                // Create expected speeds list in order
                val expectedSpeeds = result.events.indices.map { index ->
                    speeds[index] ?: "1/60"
                }

                // Create test session
                val session = TestSession(
                    cameraId = cameraId,
                    recordingFps = fps,
                    testedAt = Instant.now(),
                    avgDeviationPercent = null,
                    events = result.events,
                    expectedSpeeds = expectedSpeeds,
                    videoUri = _videoUri.value?.toString()
                )

                val sessionId = testSessionRepository.saveSession(session)

                // Calculate measured speeds and save events
                val measuredSpeeds = result.events.map { event ->
                    val duration = event.weightedDurationFrames / fps
                    1.0 / duration
                }

                testSessionRepository.saveEventsWithExpectedSpeeds(
                    sessionId = sessionId,
                    events = result.events,
                    measuredSpeeds = measuredSpeeds,
                    expectedSpeeds = expectedSpeeds
                )
                _importState.value = ImportState.Complete(sessionId)
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Failed to create session: ${e.message}")
            }
        }
    }

    /**
     * Get the created session ID (if complete).
     */
    fun getCompletedSessionId(): Long? {
        return (_importState.value as? ImportState.Complete)?.sessionId
    }

    /**
     * Reset the import flow.
     */
    fun reset() {
        _importState.value = ImportState.SelectFile
        _videoUri.value = null
        _videoInfo.value = null
        _assignedSpeeds.value = emptyMap()
        _cameraName.value = ""
        _customFrameRate.value = null
        analysisResult = null
    }
}
