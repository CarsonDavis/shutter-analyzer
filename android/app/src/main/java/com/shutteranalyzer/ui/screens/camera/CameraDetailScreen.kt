package com.shutteranalyzer.ui.screens.camera

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shutteranalyzer.domain.model.Camera
import com.shutteranalyzer.domain.model.TestSession
import com.shutteranalyzer.ui.components.AccuracyIndicator
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Camera Detail screen showing camera info and test history.
 *
 * @param cameraId The ID of the camera to display
 * @param onBackClick Callback when back button is clicked
 * @param onSessionClick Callback when a session is clicked
 * @param onTestAgain Callback when "Test Again" button is clicked
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDetailScreen(
    cameraId: Long,
    onBackClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onTestAgain: () -> Unit,
    viewModel: CameraDetailViewModel = hiltViewModel()
) {
    val camera by viewModel.camera.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsStateWithLifecycle()
    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()
    val isEditingName by viewModel.isEditingName.collectAsStateWithLifecycle()
    val editedName by viewModel.editedName.collectAsStateWithLifecycle()

    // Navigate back after deletion
    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            onBackClick()
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            cameraName = camera?.name ?: "this camera",
            onConfirm = viewModel::deleteCamera,
            onDismiss = viewModel::dismissDeleteConfirmation
        )
    }

    // Name edit dialog
    if (isEditingName) {
        EditNameDialog(
            currentName = editedName,
            onNameChange = viewModel::updateEditedName,
            onConfirm = viewModel::saveEditedName,
            onDismiss = viewModel::cancelEditingName
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        camera?.let { cam ->
            CameraDetailContent(
                camera = cam,
                sessions = sessions,
                onEditName = viewModel::startEditingName,
                onSessionClick = onSessionClick,
                onTestAgain = onTestAgain,
                onDelete = viewModel::showDeleteConfirmation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CameraDetailContent(
    camera: Camera,
    sessions: List<TestSession>,
    onEditName: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onTestAgain: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Camera header
        CameraHeader(
            camera = camera,
            onEditName = onEditName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Session history
        if (sessions.isEmpty()) {
            EmptySessionsState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            SessionList(
                sessions = sessions,
                onSessionClick = onSessionClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        // Bottom buttons
        BottomActions(
            onTestAgain = onTestAgain,
            onDelete = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraHeader(
    camera: Camera,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = camera.name,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "${camera.testCount} test${if (camera.testCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEditName) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit name"
            )
        }
    }
}

@Composable
private fun SessionList(
    sessions: List<TestSession>,
    onSessionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "TEST HISTORY",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(sessions, key = { it.id }) { session ->
            SessionCard(
                session = session,
                onClick = { onSessionClick(session.id) }
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: TestSession,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateFormatter.format(session.testedAt),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${session.events.size} speeds tested",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            session.avgDeviationPercent?.let { deviation ->
                AccuracyIndicator(deviationPercent = abs(deviation))
            }
        }
    }
}

@Composable
private fun EmptySessionsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tests yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap \"Test Again\" to record\nshutter speeds for this camera",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BottomActions(
    onTestAgain: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onTestAgain,
            modifier = Modifier.weight(1f)
        ) {
            Text("TEST AGAIN")
        }
        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("DELETE")
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    cameraName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Camera") },
        text = {
            Text("Are you sure you want to delete \"$cameraName\" and all its test history? This cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Camera Name") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Camera name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = currentName.isNotBlank()
            ) {
                Text("Save")
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
private fun CameraDetailPreview() {
    ShutterAnalyzerTheme {
        CameraDetailContent(
            camera = Camera(1, "Fuji GW690II", Instant.now(), 3),
            sessions = listOf(
                TestSession(
                    id = 1,
                    cameraId = 1,
                    recordingFps = 240.0,
                    testedAt = Instant.now(),
                    avgDeviationPercent = 3.5
                ),
                TestSession(
                    id = 2,
                    cameraId = 1,
                    recordingFps = 240.0,
                    testedAt = Instant.now().minusSeconds(86400),
                    avgDeviationPercent = 8.2
                )
            ),
            onEditName = {},
            onSessionClick = {},
            onTestAgain = {},
            onDelete = {}
        )
    }
}
