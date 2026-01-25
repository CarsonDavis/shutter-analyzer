package com.shutteranalyzer.data.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
 * Manages CameraX for preview, video recording, and real-time frame analysis.
 */
@Singleton
class ShutterCameraManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val frameAnalyzer: FrameAnalyzer
) {
    private var cameraProvider: ProcessCameraProvider? = null
    @Volatile private var videoCapture: VideoCapture<Recorder>? = null
    @Volatile private var recording: Recording? = null
    @Volatile private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()

    private var eventIndex = 0
    private var recordingStartTimestamp: Long = 0L

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

        frameAnalyzer.onCalibrationComplete = { _, _ ->
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
     * @param lifecycleOwner Lifecycle owner (typically an Activity or Fragment)
     * @param previewView PreviewView to display camera preview
     */
    fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: run {
            _cameraState.value = CameraState.Error("Camera not initialized")
            return
        }

        try {
            // Unbind any existing use cases
            provider.unbindAll()

            // Preview
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Image Analysis for real-time brightness monitoring
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, frameAnalyzer)
                }

            // Video Capture
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.FHD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                    )
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Bind to lifecycle
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
                videoCapture
            )

            _cameraState.value = CameraState.Ready
        } catch (e: Exception) {
            val errorMessage = getCameraErrorMessage(e)
            _cameraState.value = CameraState.Error(errorMessage)
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

        // Reset events for new recording
        _detectedEvents.value = emptyList()
        eventIndex = 0
        frameAnalyzer.resetEvents()

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
                        recordingStartTimestamp = System.nanoTime()
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
     * Reset the frame analyzer for a new calibration.
     */
    fun resetCalibration() {
        frameAnalyzer.reset()
        _isCalibrated.value = false
        _calibrationProgress.value = 0f
        _detectedEvents.value = emptyList()
        eventIndex = 0
    }

    /**
     * Get detected events and their timestamps.
     */
    fun getDetectedEvents(): List<EventMarker> = _detectedEvents.value

    /**
     * Get the timestamp when recording started (nanoseconds).
     */
    fun getRecordingStartTimestamp(): Long = recordingStartTimestamp

    /**
     * Get the baseline brightness from calibration.
     */
    fun getBaselineBrightness(): Double = frameAnalyzer.currentBaseline

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
