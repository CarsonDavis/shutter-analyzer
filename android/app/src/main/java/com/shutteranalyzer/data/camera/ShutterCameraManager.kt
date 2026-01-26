package com.shutteranalyzer.data.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Marker for a detected shutter event during recording.
 */
data class EventMarker(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val index: Int,
    val brightnessValues: List<Double>
)

/**
 * State of the camera manager.
 */
sealed class CameraState {
    object Uninitialized : CameraState()
    object Initializing : CameraState()
    object Ready : CameraState()
    object Recording : CameraState()
    data class Error(val message: String) : CameraState()
}

/**
 * Focus mode for the camera.
 */
enum class FocusMode {
    AUTO,
    MANUAL
}

/**
 * Manages CameraX for preview, video recording, and real-time frame analysis.
 */
@Singleton
class ShutterCameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val frameAnalyzer: FrameAnalyzer
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    @Volatile private var videoCapture: VideoCapture<Recorder>? = null
    @Volatile private var recording: Recording? = null
    @Volatile private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Store for rebinding when focus mode changes
    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    private var currentFocusMode: FocusMode = FocusMode.AUTO

    // Minimum focus distance from camera characteristics (in diopters)
    private var minFocusDistance: Float = 0f

    // Zoom state - uses CameraX native zoom
    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _minZoomRatio = MutableStateFlow(1f)
    val minZoomRatio: StateFlow<Float> = _minZoomRatio.asStateFlow()

    private val _maxZoomRatio = MutableStateFlow(MAX_DIGITAL_ZOOM)
    val maxZoomRatio: StateFlow<Float> = _maxZoomRatio.asStateFlow()

    companion object {
        // Maximum digital zoom ratio
        const val MAX_DIGITAL_ZOOM = 4f
    }

    // Focus state
    private val _isAutoFocus = MutableStateFlow(true)
    val isAutoFocus: StateFlow<Boolean> = _isAutoFocus.asStateFlow()

    private val _focusDistance = MutableStateFlow(0.5f) // Start at middle
    val focusDistance: StateFlow<Float> = _focusDistance.asStateFlow()

    // State flows
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Uninitialized)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentBrightness = MutableStateFlow(0.0)
    val currentBrightness: StateFlow<Double> = _currentBrightness.asStateFlow()

    private val _detectedEvents = MutableStateFlow<List<EventMarker>>(emptyList())
    val detectedEvents: StateFlow<List<EventMarker>> = _detectedEvents.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0f)
    val calibrationProgress: StateFlow<Float> = _calibrationProgress.asStateFlow()

    private val _isIdle = MutableStateFlow(true)
    val isIdle: StateFlow<Boolean> = _isIdle.asStateFlow()

    private val _isWaitingForCalibrationShutter = MutableStateFlow(false)
    val isWaitingForCalibrationShutter: StateFlow<Boolean> = _isWaitingForCalibrationShutter.asStateFlow()

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()

    private var eventIndex = 0

    init {
        setupFrameAnalyzerCallbacks()
    }

    private fun setupFrameAnalyzerCallbacks() {
        frameAnalyzer.onBrightnessUpdate = { brightness ->
            _currentBrightness.value = brightness
        }

        frameAnalyzer.onCalibrationProgress = { progress ->
            _calibrationProgress.value = progress
        }

        frameAnalyzer.onBaselineCalibrationComplete = { _ ->
            // Phase 1 complete - waiting for user to fire calibration shutter
            _isWaitingForCalibrationShutter.value = true
        }

        frameAnalyzer.onCalibrationComplete = { _, _ ->
            // Phase 2 complete - full calibration done, ready to detect
            _isWaitingForCalibrationShutter.value = false
            _isCalibrated.value = true
        }

        frameAnalyzer.onEventDetected = { startTimestamp, endTimestamp, brightnessValues ->
            val event = EventMarker(
                startTimestamp = startTimestamp,
                endTimestamp = endTimestamp,
                index = eventIndex++,
                brightnessValues = brightnessValues
            )
            _detectedEvents.value = _detectedEvents.value + event
        }
    }

    /**
     * Initialize the camera provider.
     */
    suspend fun initialize(): Boolean = suspendCoroutine { continuation ->
        _cameraState.value = CameraState.Initializing

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                _cameraState.value = CameraState.Ready
                continuation.resume(true)
            } catch (e: Exception) {
                val errorMessage = getCameraErrorMessage(e)
                _cameraState.value = CameraState.Error(errorMessage)
                continuation.resume(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Get a user-friendly error message for camera errors.
     */
    private fun getCameraErrorMessage(e: Exception): String {
        val message = e.message?.lowercase() ?: ""
        return when {
            message.contains("permission") ->
                "Camera permission is required. Please grant camera access in settings."
            message.contains("in use") || message.contains("busy") ->
                "Camera is being used by another app. Please close other camera apps and try again."
            message.contains("unavailable") || message.contains("not found") ->
                "Camera is unavailable. Please check if your device has a camera."
            message.contains("disconnected") ->
                "Camera was disconnected. Please try again."
            message.contains("max cameras") ->
                "Too many camera sessions open. Please close other camera apps."
            message.contains("security") ->
                "Camera access was denied. Please grant permission in settings."
            else -> "Failed to initialize camera: ${e.message ?: "Unknown error"}"
        }
    }

    /**
     * Bind camera preview and analysis to a lifecycle owner.
     *
     * Focus mode must be set at bind time using Camera2Interop.Extender because
     * CameraX overrides AF settings applied dynamically via Camera2CameraControl.
     *
     * @param lifecycleOwner Lifecycle owner (typically an Activity or Fragment)
     * @param previewView PreviewView to display camera preview
     * @param focusMode Focus mode to use (AUTO or MANUAL)
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        focusMode: FocusMode = FocusMode.AUTO
    ) {
        val provider = cameraProvider ?: run {
            _cameraState.value = CameraState.Error("Camera not initialized")
            return
        }

        // Store for rebinding when focus mode changes
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        currentFocusMode = focusMode
        _isAutoFocus.value = (focusMode == FocusMode.AUTO)

        try {
            // Unbind any existing use cases
            provider.unbindAll()

            // Build Preview with focus settings baked in
            val previewBuilder = Preview.Builder()
            applyFocusSettingsToBuilder(previewBuilder, focusMode)
            preview = previewBuilder.build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Build ImageAnalysis with same focus settings
            val analysisBuilder = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            applyFocusSettingsToBuilder(analysisBuilder, focusMode)
            imageAnalysis = analysisBuilder.build().also {
                it.setAnalyzer(cameraExecutor, frameAnalyzer)
            }

            // Video Capture (Camera2Interop.Extender not directly supported, but
            // the camera session will use the same settings as Preview/ImageAnalysis)
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                    )
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Bind to lifecycle and store camera reference
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
                videoCapture
            )

            // Get camera characteristics for manual focus range
            camera?.cameraInfo?.let { cameraInfo ->
                try {
                    val camera2Info = Camera2CameraInfo.from(cameraInfo)
                    minFocusDistance = camera2Info.getCameraCharacteristic(
                        CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
                    ) ?: 0f
                } catch (e: Exception) {
                    minFocusDistance = 0f
                }
            }

            // Reset zoom to 1x
            _zoomRatio.value = 1f

            _cameraState.value = CameraState.Ready
        } catch (e: Exception) {
            val errorMessage = getCameraErrorMessage(e)
            _cameraState.value = CameraState.Error(errorMessage)
        }
    }

    /**
     * Apply focus settings to a UseCase builder using Camera2Interop.Extender.
     * This must be done BEFORE building the UseCase, as CameraX overrides
     * AF settings applied dynamically.
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun applyFocusSettingsToBuilder(builder: Any, focusMode: FocusMode) {
        val extender = when (builder) {
            is Preview.Builder -> Camera2Interop.Extender(builder)
            is ImageAnalysis.Builder -> Camera2Interop.Extender(builder)
            else -> return
        }

        when (focusMode) {
            FocusMode.AUTO -> {
                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }
            FocusMode.MANUAL -> {
                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_OFF
                )
                // Set initial focus distance (will be updated dynamically)
                val effectiveMinFocus = if (minFocusDistance > 0f) minFocusDistance else 10f
                val focusDiopters = effectiveMinFocus * (1f - _focusDistance.value)
                extender.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    focusDiopters
                )
            }
        }
    }

    /**
     * Start recording video.
     *
     * @param onComplete Callback with the URI of the saved video
     * @param onError Callback if recording fails
     */
    fun startRecording(
        onComplete: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = videoCapture ?: run {
            onError("Video capture not initialized")
            return
        }

        // Stop any existing recording first
        recording?.stop()
        recording = null

        // Reset for new recording (including timestamp reference)
        _detectedEvents.value = emptyList()
        eventIndex = 0
        frameAnalyzer.resetForNewRecording()

        // Create output options
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "ShutterAnalyzer_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ShutterAnalyzer")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        // Start recording
        recording = capture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _isRecording.value = true
                        _cameraState.value = CameraState.Recording
                        // Note: recording start timestamp is captured from first analyzed frame
                    }
                    is VideoRecordEvent.Finalize -> {
                        _isRecording.value = false
                        _cameraState.value = CameraState.Ready

                        if (event.hasError()) {
                            onError("Recording failed: ${event.cause?.message}")
                        } else {
                            event.outputResults.outputUri.let { uri ->
                                onComplete(uri)
                            }
                        }
                    }
                }
            }
    }

    /**
     * Stop recording video.
     */
    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    /**
     * Reset the frame analyzer for a new calibration (returns to Idle state).
     */
    fun resetCalibration() {
        frameAnalyzer.reset()
        _isIdle.value = true
        _isCalibrated.value = false
        _isWaitingForCalibrationShutter.value = false
        _calibrationProgress.value = 0f
        _detectedEvents.value = emptyList()
        eventIndex = 0
    }

    /**
     * Start baseline calibration from idle state.
     * Transitions from Idle → CalibratingBaseline.
     *
     * @return true if calibration was started, false if not in Idle state
     */
    fun startBaselineCalibration(): Boolean {
        val started = frameAnalyzer.startBaselineCalibration()
        if (started) {
            _isIdle.value = false
        }
        return started
    }

    /**
     * Lock auto-exposure to prevent brightness fluctuations during detection.
     * Should be called when detection begins.
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun lockExposure() {
        val cam = camera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            camera2Control.setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                    .build()
            )
        } catch (e: Exception) {
            // Ignore - AE lock not available on this device
        }
    }

    /**
     * Unlock auto-exposure.
     * Called when detection/recording stops.
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun unlockExposure() {
        val cam = camera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            camera2Control.setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, false)
                    .build()
            )
        } catch (e: Exception) {
            // Ignore - AE lock not available on this device
        }
    }

    /**
     * Get detected events and their timestamps.
     */
    fun getDetectedEvents(): List<EventMarker> = _detectedEvents.value

    /**
     * Get the timestamp when recording started (nanoseconds).
     * Uses the first analyzed frame's camera timestamp for accurate frame indexing.
     */
    fun getRecordingStartTimestamp(): Long = frameAnalyzer.recordingStartTimestamp

    /**
     * Get the baseline brightness from calibration.
     */
    fun getBaselineBrightness(): Double = frameAnalyzer.currentBaseline

    /**
     * Set the camera zoom ratio.
     * Uses CameraX native zoom which is simpler and well-supported.
     *
     * @param ratio Zoom ratio between 1x and MAX_DIGITAL_ZOOM
     */
    fun setZoom(ratio: Float) {
        val clampedRatio = ratio.coerceIn(1f, MAX_DIGITAL_ZOOM)
        _zoomRatio.value = clampedRatio
        camera?.cameraControl?.setZoomRatio(clampedRatio)
    }

    /**
     * Enable autofocus mode.
     * Requires rebinding the camera because CameraX overrides AF settings
     * applied dynamically via Camera2CameraControl.
     *
     * Note: Cannot change focus mode while recording is active.
     */
    fun enableAutoFocus() {
        if (currentFocusMode == FocusMode.AUTO) return // Already in auto mode
        if (_isRecording.value) return // Cannot rebind while recording

        val lifecycleOwner = currentLifecycleOwner ?: return
        val previewView = currentPreviewView ?: return

        // Rebind with autofocus mode
        bindPreview(lifecycleOwner, previewView, FocusMode.AUTO)
    }

    /**
     * Enable manual focus mode and set initial focus distance.
     * Requires rebinding the camera because CameraX overrides AF settings
     * applied dynamically via Camera2CameraControl.
     *
     * Note: Cannot change focus mode while recording is active.
     *
     * @param distance Focus distance (0 = near/macro, 1 = infinity)
     */
    fun enableManualFocus(distance: Float = _focusDistance.value) {
        if (_isRecording.value) return // Cannot rebind while recording

        _focusDistance.value = distance.coerceIn(0f, 1f)

        val lifecycleOwner = currentLifecycleOwner ?: return
        val previewView = currentPreviewView ?: return

        // Rebind with manual focus mode
        bindPreview(lifecycleOwner, previewView, FocusMode.MANUAL)
    }

    /**
     * Set manual focus distance.
     * Only works when already in manual focus mode (after enableManualFocus was called).
     *
     * @param distance Focus distance (0 = near/macro, 1 = infinity)
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun setManualFocus(distance: Float) {
        if (currentFocusMode != FocusMode.MANUAL) {
            // If not in manual mode, switch to it first
            enableManualFocus(distance)
            return
        }

        _focusDistance.value = distance.coerceIn(0f, 1f)

        // Dynamically update focus distance (this works because AF_MODE=OFF was baked in)
        val cam = camera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val effectiveMinFocus = if (minFocusDistance > 0f) minFocusDistance else 10f
            val focusDiopters = effectiveMinFocus * (1f - _focusDistance.value)

            camera2Control.setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDiopters)
                    .build()
            )
        } catch (e: Exception) {
            // Ignore - focus control not available
        }
    }

    /**
     * Release camera resources.
     */
    fun release() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
