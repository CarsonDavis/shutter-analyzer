package com.shutteranalyzer.ui.screens.videoimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shutteranalyzer.analysis.model.ShutterEvent
import com.shutteranalyzer.data.video.VideoInfo
import com.shutteranalyzer.ui.screens.setup.STANDARD_SPEEDS
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme

/**
 * Import screen for analyzing existing video files.
 *
 * @param onBackClick Callback when back is pressed
 * @param onComplete Callback when import is complete with session ID
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBackClick: () -> Unit,
    onComplete: (Long) -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val videoInfo by viewModel.videoInfo.collectAsStateWithLifecycle()
    val assignedSpeeds by viewModel.assignedSpeeds.collectAsStateWithLifecycle()
    val cameraName by viewModel.cameraName.collectAsStateWithLifecycle()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectVideo(it) }
    }

    // Handle completion
    LaunchedEffect(importState) {
        if (importState is ImportState.Complete) {
            val sessionId = (importState as ImportState.Complete).sessionId
            onComplete(sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Video") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = importState) {
                is ImportState.SelectFile -> {
                    FileSelectionStep(
                        onSelectFile = { filePickerLauncher.launch("video/*") }
                    )
                }

                is ImportState.Loading -> {
                    LoadingState(message = "Loading video...")
                }

                is ImportState.VideoSelected -> {
                    VideoSelectedStep(
                        videoInfo = state.videoInfo,
                        cameraName = cameraName,
                        onCameraNameChange = viewModel::updateCameraName,
                        onAnalyze = viewModel::analyzeVideo,
                        onSelectDifferent = { filePickerLauncher.launch("video/*") }
                    )
                }

                is ImportState.Analyzing -> {
                    AnalyzingState(progress = state.progress)
                }

                is ImportState.AssignSpeeds -> {
                    SpeedAssignmentStep(
                        events = state.events,
                        assignedSpeeds = assignedSpeeds,
                        onSpeedAssign = viewModel::assignSpeed,
                        onConfirm = { viewModel.createSession() }
                    )
                }

                is ImportState.Complete -> {
                    // Will navigate via LaunchedEffect
                    LoadingState(message = "Creating session...")
                }

                is ImportState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = viewModel::reset
                    )
                }
            }
        }
    }
}

@Composable
private fun FileSelectionStep(
    onSelectFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = "Video library",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Import a Video",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select a slow-motion video of your camera's shutter to analyze",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onSelectFile) {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = "Select video file",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("SELECT VIDEO")
        }
    }
}

@Composable
private fun VideoSelectedStep(
    videoInfo: VideoInfo,
    cameraName: String,
    onCameraNameChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onSelectDifferent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Video info card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "VIDEO INFORMATION",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(12.dp))

                VideoInfoRow("Duration", formatDuration(videoInfo.durationMs))
                VideoInfoRow("Frame Rate", "${videoInfo.frameRate.toInt()} fps")
                VideoInfoRow("Resolution", "${videoInfo.width} x ${videoInfo.height}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Camera name input
        OutlinedTextField(
            value = cameraName,
            onValueChange = onCameraNameChange,
            label = { Text("Camera name (optional)") },
            placeholder = { Text("e.g., Fuji GW690II") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSelectDifferent,
                modifier = Modifier.weight(1f)
            ) {
                Text("DIFFERENT VIDEO")
            }
            Button(
                onClick = onAnalyze,
                modifier = Modifier.weight(1f)
            ) {
                Text("ANALYZE")
            }
        }
    }
}

@Composable
private fun VideoInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AnalyzingState(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analyzing Video...",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedAssignmentStep(
    events: List<ShutterEvent>,
    assignedSpeeds: Map<Int, String>,
    onSpeedAssign: (Int, String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ASSIGN SHUTTER SPEEDS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Found ${events.size} shutter events. Assign the expected speed for each.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Event list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(events) { index, event ->
                EventSpeedCard(
                    eventNumber = index + 1,
                    event = event,
                    selectedSpeed = assignedSpeeds[index] ?: "1/60",
                    onSpeedSelect = { speed -> onSpeedAssign(index, speed) }
                )
            }
        }

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("REVIEW EVENTS")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventSpeedCard(
    eventNumber: Int,
    event: ShutterEvent,
    selectedSpeed: String,
    onSpeedSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Event $eventNumber",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${event.durationFrames} frames",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSpeed,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(120.dp),
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    STANDARD_SPEEDS.forEach { speed ->
                        DropdownMenuItem(
                            text = { Text(speed) },
                            onClick = {
                                onSpeedSelect(speed)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error occurred",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("TRY AGAIN")
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}

@Preview(showBackground = true)
@Composable
private fun FileSelectionPreview() {
    ShutterAnalyzerTheme {
        FileSelectionStep(onSelectFile = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyzingPreview() {
    ShutterAnalyzerTheme {
        AnalyzingState(progress = 0.45f)
    }
}
