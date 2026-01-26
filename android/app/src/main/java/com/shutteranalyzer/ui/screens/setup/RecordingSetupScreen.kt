package com.shutteranalyzer.ui.screens.setup

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import kotlinx.coroutines.launch

/**
 * Recording setup screen for configuring a new test.
 *
 * @param onBackClick Callback when back button is clicked
 * @param onStartRecording Callback when recording should start, with session ID
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordingSetupScreen(
    onBackClick: () -> Unit,
    onStartRecording: (Long) -> Unit,
    viewModel: RecordingSetupViewModel = hiltViewModel()
) {
    val cameraName by viewModel.cameraName.collectAsStateWithLifecycle()
    val useStandardSpeeds by viewModel.useStandardSpeeds.collectAsStateWithLifecycle()
    val selectedSpeeds by viewModel.selectedSpeeds.collectAsStateWithLifecycle()
    val deviceFps by viewModel.deviceFps.collectAsStateWithLifecycle()
    val accuracyDescription by viewModel.accuracyDescription.collectAsStateWithLifecycle()
    val showSpeedPicker by viewModel.showSpeedPicker.collectAsStateWithLifecycle()
    val showSetupReminder by viewModel.showSetupReminder.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    // Permission state
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showOpenSettingsDialog by remember { mutableStateOf(false) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                val sessionId = viewModel.createSession()
                onStartRecording(sessionId)
            }
        } else {
            // Check if we should show rationale or if permanently denied
            val activity = context as? android.app.Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
                )
            ) {
                // Permission permanently denied - show settings dialog
                showOpenSettingsDialog = true
            } else {
                // Permission denied but can ask again
                showPermissionDeniedDialog = true
            }
        }
    }

    // Handle permission request with rationale check
    fun requestCameraPermission() {
        val activity = context as? android.app.Activity
        if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
        ) {
            showPermissionRationaleDialog = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Permission denied dialog - can request again
    if (showPermissionDeniedDialog) {
        PermissionDeniedDialog(
            onDismiss = { showPermissionDeniedDialog = false },
            onRequestAgain = {
                showPermissionDeniedDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    // Permission rationale dialog
    if (showPermissionRationaleDialog) {
        PermissionRationaleDialog(
            onDismiss = { showPermissionRationaleDialog = false },
            onContinue = {
                showPermissionRationaleDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    // Settings dialog - permission permanently denied
    if (showOpenSettingsDialog) {
        OpenSettingsDialog(
            onDismiss = { showOpenSettingsDialog = false },
            onOpenSettings = {
                showOpenSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NEW TEST (v4)") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Camera name input
            Text(
                text = "CAMERA NAME (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cameraName,
                onValueChange = viewModel::updateCameraName,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. \"Fuji GW690II\"") },
                singleLine = true
            )
            Text(
                text = "Leave blank for quick test",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Speed set selection
            Text(
                text = "SHUTTER SPEEDS TO TEST",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.selectableGroup()) {
                SpeedSetOption(
                    title = "Standard Set",
                    description = "1/1000, 1/500, 1/250, 1/125, 1/60, 1/30, 1/15, 1/8, 1/4, 1/2, 1s",
                    selected = useStandardSpeeds,
                    onClick = { viewModel.setUseStandardSpeeds(true) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SpeedSetOption(
                    title = "Custom",
                    description = if (!useStandardSpeeds) {
                        selectedSpeeds.joinToString(", ")
                    } else {
                        "Choose specific speeds"
                    },
                    selected = !useStandardSpeeds,
                    onClick = {
                        viewModel.setUseStandardSpeeds(false)
                        viewModel.openSpeedPicker()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Setup reminder
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Setup information",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "  Setup reminder",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = viewModel::toggleSetupReminder) {
                            Icon(
                                imageVector = if (showSetupReminder) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showSetupReminder) "Collapse" else "Expand"
                            )
                        }
                    }

                    AnimatedVisibility(visible = showSetupReminder) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            SetupReminderItem("Camera on tripod with back open")
                            SetupReminderItem("Phone viewing through shutter")
                            SetupReminderItem("Bright light behind camera")
                        }
                    }
                }
            }

            // Device FPS info
            if (deviceFps > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Device: ${deviceFps}fps - $accuracyDescription",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Start recording button
            Button(
                onClick = { requestCameraPermission() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedSpeeds.isNotEmpty()
            ) {
                Text("START RECORDING")
            }
        }
    }

    // Speed picker bottom sheet
    if (showSpeedPicker) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSpeedPicker,
            sheetState = sheetState
        ) {
            SpeedPickerContent(
                selectedSpeeds = selectedSpeeds,
                onToggleSpeed = viewModel::toggleSpeed,
                onDone = viewModel::closeSpeedPicker
            )
        }
    }
}

@Composable
private fun SpeedSetOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SetupReminderItem(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedPickerContent(
    selectedSpeeds: List<String>,
    onToggleSpeed: (String) -> Unit,
    onDone: () -> Unit
) {
    val allSpeeds = listOf(
        "1/1000", "1/500", "1/250", "1/125", "1/60",
        "1/30", "1/15", "1/8", "1/4", "1/2", "1s", "2s", "4s", "B"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "SELECT SPEEDS TO TEST",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allSpeeds.forEach { speed ->
                SpeedCheckbox(
                    speed = speed,
                    checked = speed in selectedSpeeds,
                    onCheckedChange = { onToggleSpeed(speed) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("DONE")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SpeedCheckbox(
    speed: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .selectable(
                selected = checked,
                onClick = onCheckedChange,
                role = Role.Checkbox
            )
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = speed,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * Dialog shown when camera permission is denied but can be requested again.
 */
@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onRequestAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Camera Permission Required")
        },
        text = {
            Text(
                "Shutter Analyzer needs camera access to record and analyze your camera's shutter. " +
                        "Without this permission, you won't be able to perform shutter speed tests.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onRequestAgain) {
                Text("Try Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog explaining why camera permission is needed (shown before first request if rationale applies).
 */
@Composable
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Why Camera Access?")
        },
        text = {
            Text(
                "Shutter Analyzer uses your phone's camera to record high-speed video of your " +
                        "camera's shutter opening and closing. This allows us to measure the actual " +
                        "shutter speed with high precision.\n\n" +
                        "The video is processed on your device and is not sent anywhere.",
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog shown when permission is permanently denied, directing user to settings.
 */
@Composable
private fun OpenSettingsDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Permission Required")
        },
        text = {
            Text(
                "Camera permission has been permanently denied. To use Shutter Analyzer, " +
                        "please enable camera access in your device settings.\n\n" +
                        "Go to Settings → Permissions → Camera → Allow",
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SpeedSetOptionPreview() {
    ShutterAnalyzerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpeedSetOption(
                title = "Standard Set",
                description = "1/1000, 1/500, 1/250...",
                selected = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            SpeedSetOption(
                title = "Custom",
                description = "Choose specific speeds",
                selected = false,
                onClick = {}
            )
        }
    }
}
