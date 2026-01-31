package com.shutteranalyzer.ui.screens.recording

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shutteranalyzer.data.camera.CameraState
import com.shutteranalyzer.ui.components.BrightnessIndicator
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme

/**
 * Recording screen for capturing shutter events.
 *
 * @param sessionId The test session ID
 * @param onRecordingComplete Callback when recording is complete
 * @param onCancel Callback when recording is cancelled
 * @param viewModel The ViewModel for this screen
 */
@Composable
fun RecordingScreen(
    sessionId: Long,
    onRecordingComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val currentBrightness by viewModel.currentBrightness.collectAsStateWithLifecycle()
    val calibrationProgress by viewModel.calibrationProgress.collectAsStateWithLifecycle()
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val currentSpeedIndex by viewModel.currentSpeedIndex.collectAsStateWithLifecycle()
    val expectedSpeeds by viewModel.expectedSpeeds.collectAsStateWithLifecycle()
    val recordingFps by viewModel.recordingFps.collectAsStateWithLifecycle()
    val detectedEvents by viewModel.detectedEvents.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val minZoomRatio by viewModel.minZoomRatio.collectAsStateWithLifecycle()
    val maxZoomRatio by viewModel.maxZoomRatio.collectAsStateWithLifecycle()
    val isAutoFocus by viewModel.isAutoFocus.collectAsStateWithLifecycle()
    val focusDistance by viewModel.focusDistance.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Track if camera is initialized and preview is bound
    var cameraInitialized by remember { mutableStateOf(false) }
    var previewBound by remember { mutableStateOf(false) }

    // Initialize camera
    LaunchedEffect(Unit) {
        cameraInitialized = viewModel.initializeCamera()
    }

    // Start recording only after camera is initialized AND preview is bound
    // Small delay to let Camera2Interop settings stabilize before recording
    LaunchedEffect(cameraInitialized, previewBound) {
        if (cameraInitialized && previewBound) {
            kotlinx.coroutines.delay(500) // Wait for camera to stabilize
            viewModel.startRecording()
        }
    }

    // Handle completion
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.Complete) {
            onRecordingComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview - only show after camera is initialized
        if (cameraInitialized) {
            CameraPreviewView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                onPreviewBound = { previewBound = true }
            )
        }

        // Overlay based on state
        when (val state = recordingState) {
            is RecordingState.Initializing -> {
                LoadingOverlay(message = "Initializing camera...")
            }
            is RecordingState.SettingUp -> {
                SettingUpOverlay(
                    zoomRatio = zoomRatio,
                    minZoomRatio = minZoomRatio,
                    maxZoomRatio = maxZoomRatio,
                    onZoomChange = viewModel::setZoom,
                    isAutoFocus = isAutoFocus,
                    focusDistance = focusDistance,
                    onAutoFocusClick = {
                        if (isAutoFocus) {
                            viewModel.enableManualFocus()
                        } else {
                            viewModel.enableAutoFocus()
                        }
                    },
                    onFocusChange = viewModel::setManualFocus,
                    onBeginDetection = viewModel::beginDetection,
                    onCancel = onCancel
                )
            }
            is RecordingState.ReadyForBaseline -> {
                ReadyForBaselineOverlay(
                    onReady = viewModel::startBaselineCalibration,
                    onCancel = onCancel
                )
            }
            is RecordingState.CalibratingBaseline -> {
                CalibratingOverlay(
                    progress = calibrationProgress,
                    message = "Establishing baseline...",
                    onCancel = onCancel
                )
            }
            is RecordingState.WaitingForCalibrationShutter -> {
                WaitingForCalibrationShutterOverlay(
                    onCancel = onCancel
                )
            }
            is RecordingState.WaitingForShutter -> {
                WaitingForShutterOverlay(
                    speed = state.speed,
                    currentIndex = state.index,
                    totalSpeeds = state.total,
                    fps = recordingFps,
                    brightness = currentBrightness,
                    onRedo = viewModel::redoLastEvent,
                    onSkip = viewModel::skipSpeed,
                    onDone = viewModel::finishEarly
                )
            }
            is RecordingState.EventDetected -> {
                EventDetectedOverlay(
                    speed = state.speed,
                    currentIndex = state.index,
                    totalSpeeds = expectedSpeeds.size,
                    fps = recordingFps
                )
            }
            is RecordingState.Analyzing -> {
                AnalyzingOverlay(progress = state.progress)
            }
            is RecordingState.Complete -> {
                // Will navigate away
            }
            is RecordingState.Error -> {
                ErrorOverlay(
                    message = state.message,
                    onRetry = viewModel::startRecording,
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewView(
    viewModel: RecordingViewModel,
    modifier: Modifier = Modifier,
    onPreviewBound: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Bind preview when the composable is first created
    DisposableEffect(lifecycleOwner) {
        viewModel.bindCameraPreview(lifecycleOwner, previewView)
        onPreviewBound()
        onDispose { }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun AnalyzingOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = { progress },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Analyzing video...",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingUpOverlay(
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    isAutoFocus: Boolean,
    focusDistance: Float,
    onAutoFocusClick: () -> Unit,
    onFocusChange: (Float) -> Unit,
    onBeginDetection: () -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Cancel button in top left
        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Cancel", color = Color.White)
        }

        // Vertical sliders on the right side - zoom on top, focus below
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom slider (top)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom control",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${maxZoomRatio.toInt()}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Vertical zoom slider
                VerticalSlider(
                    value = zoomRatio,
                    onValueChange = onZoomChange,
                    valueRange = minZoomRatio..maxZoomRatio,
                    modifier = Modifier
                        .width(44.dp)
                        .height(150.dp)
                )

                Text(
                    text = "1x",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Focus slider (bottom)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                // Autofocus icon
                IconButton(
                    onClick = onAutoFocusClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = if (isAutoFocus) "Autofocus enabled" else "Enable autofocus",
                        tint = if (isAutoFocus) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Far",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Vertical focus slider
                VerticalSlider(
                    value = focusDistance,
                    onValueChange = onFocusChange,
                    valueRange = 0f..1f,
                    enabled = !isAutoFocus,
                    modifier = Modifier
                        .width(44.dp)
                        .height(150.dp)
                )

                Text(
                    text = "Near",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Begin Detecting button centered at bottom
        Button(
            onClick = onBeginDetection,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("Begin Detecting")
        }
    }
}

@Composable
private fun CalibratingOverlay(
    progress: Float,
    message: String = "Calibrating...",
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hold camera steady",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

@Composable
private fun ReadyForBaselineOverlay(
    onReady: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lightbulb icon
                    Text(
                        text = "Prepare for Calibration",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Before calibrating, ensure:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checklist items
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ChecklistItem(text = "Light source is ON")
                        ChecklistItem(text = "Phone can see back of shutter")
                        ChecklistItem(text = "Aperture fully open")
                        ChecklistItem(text = "Shutter is CLOSED")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "The app will measure the dark baseline for 5 seconds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ready")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

@Composable
private fun ChecklistItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = AccuracyGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@Composable
private fun WaitingForCalibrationShutterOverlay(
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Fire calibration shutter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Fire Shutter Once",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "to calibrate detection threshold",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "(This event will not be recorded)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

@Composable
private fun WaitingForShutterOverlay(
    speed: String,
    currentIndex: Int,
    totalSpeeds: Int,
    fps: Int,
    brightness: Double,
    onRedo: () -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${fps}fps",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Text(
                text = "${currentIndex + 1} of $totalSpeeds",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Speed prompt
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = speed,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fire shutter now",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onRedo,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Redo last measurement")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Redo")
            }

            OutlinedButton(
                onClick = onSkip,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Skip this speed")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Skip")
            }

            Button(onClick = onDone) {
                Icon(Icons.Default.Stop, contentDescription = "Finish recording")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Done")
            }
        }
    }
}

@Composable
private fun EventDetectedOverlay(
    speed: String,
    currentIndex: Int,
    totalSpeeds: Int,
    fps: Int
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${fps}fps",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Text(
                text = "${currentIndex + 1} of $totalSpeeds",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Detected confirmation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = AccuracyGreen.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Shutter event detected successfully",
                        tint = AccuracyGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = speed,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DETECTED!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(80.dp)) // Space for controls
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

/**
 * A vertical slider that properly handles touch input.
 * Uses layout rotation to display horizontally-designed Slider vertically.
 */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // We use a layout modifier to swap width and height, then rotate the content
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier
            .graphicsLayer {
                rotationZ = -90f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                // Swap width and height constraints
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                // Report swapped dimensions
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WaitingForShutterOverlayPreview() {
    ShutterAnalyzerTheme {
        WaitingForShutterOverlay(
            speed = "1/500",
            currentIndex = 2,
            totalSpeeds = 11,
            fps = 240,
            brightness = 50.0,
            onRedo = {},
            onSkip = {},
            onDone = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun EventDetectedOverlayPreview() {
    ShutterAnalyzerTheme {
        EventDetectedOverlay(
            speed = "1/500",
            currentIndex = 2,
            totalSpeeds = 11,
            fps = 240
        )
    }
}
