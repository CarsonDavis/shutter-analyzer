package com.shutteranalyzer.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.data.camera.SlowMotionCapabilityChecker
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
import javax.inject.Inject

/**
 * Standard shutter speeds available for testing.
 */
val STANDARD_SPEEDS = listOf(
    "1/1000", "1/500", "1/250", "1/125", "1/60",
    "1/30", "1/15", "1/8", "1/4", "1/2", "1s"
)

/**
 * ViewModel for the Recording Setup screen.
 */
@HiltViewModel
class RecordingSetupViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val testSessionRepository: TestSessionRepository,
    private val slowMotionChecker: SlowMotionCapabilityChecker
) : ViewModel() {

    /**
     * Camera name input.
     */
    val cameraName = MutableStateFlow("")

    /**
     * Whether to use the standard speed set.
     */
    val useStandardSpeeds = MutableStateFlow(true)

    /**
     * Selected speeds (for custom mode).
     */
    val selectedSpeeds = MutableStateFlow<List<String>>(STANDARD_SPEEDS)

    /**
     * Device FPS capability.
     */
    private val _deviceFps = MutableStateFlow(30)
    val deviceFps: StateFlow<Int> = _deviceFps.asStateFlow()

    /**
     * Accuracy description based on device FPS.
     */
    private val _accuracyDescription = MutableStateFlow("")
    val accuracyDescription: StateFlow<String> = _accuracyDescription.asStateFlow()

    /**
     * Whether the speed picker dialog is shown.
     */
    val showSpeedPicker = MutableStateFlow(false)

    /**
     * Whether setup reminder is expanded.
     */
    val showSetupReminder = MutableStateFlow(true)

    init {
        checkDeviceCapabilities()
    }

    private fun checkDeviceCapabilities() {
        viewModelScope.launch {
            val capabilities = slowMotionChecker.getCapabilities()
            _deviceFps.value = capabilities.maxFps
            _accuracyDescription.value = slowMotionChecker.getAccuracyDescription(capabilities.maxFps)
        }
    }

    /**
     * Update the camera name.
     */
    fun updateCameraName(name: String) {
        cameraName.value = name
    }

    /**
     * Toggle between standard and custom speed sets.
     */
    fun setUseStandardSpeeds(standard: Boolean) {
        useStandardSpeeds.value = standard
        if (standard) {
            selectedSpeeds.value = STANDARD_SPEEDS
        }
    }

    /**
     * Toggle a speed selection.
     */
    fun toggleSpeed(speed: String) {
        val current = selectedSpeeds.value.toMutableList()
        if (speed in current) {
            current.remove(speed)
        } else {
            // Maintain order
            val allSpeeds = STANDARD_SPEEDS + listOf("2s", "4s", "B")
            current.add(speed)
            current.sortBy { allSpeeds.indexOf(it) }
        }
        selectedSpeeds.value = current
    }

    /**
     * Open the speed picker dialog.
     */
    fun openSpeedPicker() {
        showSpeedPicker.value = true
    }

    /**
     * Close the speed picker dialog.
     */
    fun closeSpeedPicker() {
        showSpeedPicker.value = false
    }

    /**
     * Toggle setup reminder visibility.
     */
    fun toggleSetupReminder() {
        showSetupReminder.value = !showSetupReminder.value
    }

    /**
     * Create a new test session and return its ID.
     * Also creates a camera if a name is provided.
     */
    suspend fun createSession(): Long {
        // Create camera if name is provided
        val cameraId = if (cameraName.value.isNotBlank()) {
            val camera = Camera(
                name = cameraName.value.trim(),
                createdAt = Instant.now()
            )
            cameraRepository.saveCamera(camera)
        } else {
            0L // No camera associated
        }

        // Create test session with expected speeds
        val session = TestSession(
            cameraId = cameraId,
            recordingFps = _deviceFps.value.toDouble(),
            testedAt = Instant.now(),
            avgDeviationPercent = null,
            expectedSpeeds = selectedSpeeds.value
        )

        return testSessionRepository.saveSession(session)
    }

    /**
     * Get the list of speeds to test.
     */
    fun getSpeedsToTest(): List<String> = selectedSpeeds.value
}
