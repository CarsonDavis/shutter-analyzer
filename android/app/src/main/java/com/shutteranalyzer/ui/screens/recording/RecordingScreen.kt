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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Initialize camera
    LaunchedEffect(Unit) {
        val success = viewModel.initializeCamera()
        if (success) {
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
        // Camera preview
        CameraPreviewView(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay based on state
        when (val state = recordingState) {
            is RecordingState.Initializing -> {
                LoadingOverlay(message = "Initializing camera...")
            }
            is RecordingState.Calibrating -> {
                CalibratingOverlay(
                    progress = calibrationProgress,
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
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
        update = { view ->
            // Binding is handled by the camera manager
        }
    )

    // Note: In a real implementation, we'd need to call cameraManager.bindPreview here
    // This requires access to the cameraManager from the viewModel
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
private fun CalibratingOverlay(
    progress: Float,
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
                        text = "Calibrating...",
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
