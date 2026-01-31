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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Standard shutter speeds available for testing (default set).
 */
val STANDARD_SPEEDS = listOf(
    "1/1000", "1/500", "1/250", "1/125", "1/60",
    "1/30", "1/15", "1/8", "1/4", "1/2", "1s"
)

/**
 * All available shutter speeds for the Add/Remove picker (1/8000 to 8s).
 */
val ALL_SPEEDS = listOf(
    "1/8000", "1/4000", "1/2000", "1/1000", "1/500", "1/250", "1/125", "1/60",
    "1/30", "1/15", "1/8", "1/4", "1/2", "1s", "2s", "4s", "8s"
)

/**
 * Speed selection mode options.
 */
enum class SpeedSelectionMode {
    STANDARD,      // Use STANDARD_SPEEDS as-is
    ADD_REMOVE,    // Checkbox picker to add/remove from full list
    ENTER_CUSTOM   // Text input for custom speeds
}

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
     * Speed selection mode.
     */
    val speedSelectionMode = MutableStateFlow(SpeedSelectionMode.STANDARD)

    /**
     * Selected speeds (for add/remove mode).
     */
    val selectedSpeeds = MutableStateFlow<List<String>>(STANDARD_SPEEDS)

    /**
     * Custom speed text input (for enter custom mode).
     */
    val customSpeedText = MutableStateFlow("")

    /**
     * Parsed custom speeds from text input.
     */
    val parsedCustomSpeeds = MutableStateFlow<List<String>>(emptyList())

    /**
     * Error message for custom speed parsing.
     */
    val customSpeedError = MutableStateFlow<String?>(null)

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
     * Set the speed selection mode.
     */
    fun setSpeedSelectionMode(mode: SpeedSelectionMode) {
        speedSelectionMode.value = mode
        when (mode) {
            SpeedSelectionMode.STANDARD -> {
                selectedSpeeds.value = STANDARD_SPEEDS
            }
            SpeedSelectionMode.ADD_REMOVE -> {
                // Keep current selection or reset to standard
                if (selectedSpeeds.value.isEmpty()) {
                    selectedSpeeds.value = STANDARD_SPEEDS
                }
            }
            SpeedSelectionMode.ENTER_CUSTOM -> {
                // Keep existing custom text
            }
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
            // Maintain order based on ALL_SPEEDS
            current.add(speed)
            current.sortBy { ALL_SPEEDS.indexOf(it).let { idx -> if (idx == -1) Int.MAX_VALUE else idx } }
        }
        selectedSpeeds.value = current
    }

    /**
     * Update custom speed text and parse it.
     */
    fun updateCustomSpeedText(text: String) {
        customSpeedText.value = text
        parseCustomSpeeds(text)
    }

    /**
     * Parse custom speeds from comma-separated text.
     * Valid formats: 1/xxx, xs (e.g., 1/500, 1/60, 1s, 2s)
     */
    private fun parseCustomSpeeds(text: String) {
        if (text.isBlank()) {
            parsedCustomSpeeds.value = emptyList()
            customSpeedError.value = null
            return
        }

        val speeds = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val validSpeeds = mutableListOf<String>()
        val invalidSpeeds = mutableListOf<String>()

        val fractionPattern = Regex("^1/\\d+$")
        val secondsPattern = Regex("^\\d+s$")

        for (speed in speeds) {
            if (fractionPattern.matches(speed) || secondsPattern.matches(speed)) {
                validSpeeds.add(speed)
            } else {
                invalidSpeeds.add(speed)
            }
        }

        parsedCustomSpeeds.value = validSpeeds
        customSpeedError.value = if (invalidSpeeds.isNotEmpty()) {
            "Invalid: ${invalidSpeeds.joinToString(", ")}"
        } else {
            null
        }
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
     * Always creates a camera - uses provided name or generates default.
     */
    suspend fun createSession(): Long {
        // Always create a camera - use provided name or generate default
        val finalName = cameraName.value.trim().ifBlank {
            "Test ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d HH:mm"))}"
        }
        val camera = Camera(
            name = finalName,
            createdAt = Instant.now()
        )
        val cameraId = cameraRepository.saveCamera(camera)

        // Create test session with expected speeds
        val session = TestSession(
            cameraId = cameraId,
            recordingFps = _deviceFps.value.toDouble(),
            testedAt = Instant.now(),
            avgDeviationPercent = null,
            expectedSpeeds = getSpeedsToTest()
        )

        return testSessionRepository.saveSession(session)
    }

    /**
     * Get the list of speeds to test based on current mode.
     */
    fun getSpeedsToTest(): List<String> = when (speedSelectionMode.value) {
        SpeedSelectionMode.STANDARD -> STANDARD_SPEEDS
        SpeedSelectionMode.ADD_REMOVE -> selectedSpeeds.value
        SpeedSelectionMode.ENTER_CUSTOM -> parsedCustomSpeeds.value
    }

    /**
     * Check if the current speed selection is valid (non-empty).
     */
    fun isSpeedSelectionValid(): Boolean = getSpeedsToTest().isNotEmpty()
}
