package com.shutteranalyzer.ui.screens.home

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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.shutteranalyzer.domain.model.Camera
import com.shutteranalyzer.ui.components.CameraCard
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import java.time.Instant

/**
 * Home screen displaying the camera list.
 *
 * @param onNewTestClick Callback when "New Test" button is clicked
 * @param onCameraClick Callback when a camera is clicked
 * @param onSettingsClick Callback when settings icon is clicked
 * @param onImportClick Callback when "Import" button is clicked
 * @param onTutorialClick Callback when "Tutorial" is clicked from help menu
 * @param onTheoryClick Callback when "How It Works" is clicked from help menu
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewTestClick: () -> Unit,
    onCameraClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onImportClick: () -> Unit,
    onTutorialClick: () -> Unit = {},
    onTheoryClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val cameras by viewModel.cameras.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()

    var helpMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHUTTER ANALYZER") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open settings"
                        )
                    }
                    Box {
                        IconButton(onClick = { helpMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = "Help"
                            )
                        }
                        DropdownMenu(
                            expanded = helpMenuExpanded,
                            onDismissRequest = { helpMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tutorial") },
                                onClick = {
                                    helpMenuExpanded = false
                                    onTutorialClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("How It Works") },
                                onClick = {
                                    helpMenuExpanded = false
                                    onTheoryClick()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomButtons(
                onNewTestClick = onNewTestClick,
                onImportClick = onImportClick
            )
        }
    ) { paddingValues ->
        if (isEmpty) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            CameraList(
                cameras = cameras,
                onCameraClick = onCameraClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CameraList(
    cameras: List<Camera>,
    onCameraClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Measure your film camera's shutter speeds using your phone's slow-motion video.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MY CAMERAS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(cameras, key = { it.id }) { camera ->
            CameraCard(
                camera = camera,
                onClick = { onCameraClick(camera.id) }
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Subtitle at the top
        Text(
            text = "Measure your film camera's shutter speeds using your phone's slow-motion video.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // Centered empty state content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No cameras tested yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap \"New Test\" to measure\nyour first camera's shutter speeds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BottomButtons(
    onNewTestClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onNewTestClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new test",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("NEW CAMERA")
        }

        OutlinedButton(
            onClick = onImportClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = "Import video",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("IMPORT")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    ShutterAnalyzerTheme {
        EmptyState(modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraListPreview() {
    ShutterAnalyzerTheme {
        CameraList(
            cameras = listOf(
                Camera(1, "Fuji GW690II", Instant.now(), 3),
                Camera(2, "Nikon F3", Instant.now(), 1),
                Camera(3, "Mamiya RB67", Instant.now(), 2)
            ),
            onCameraClick = {}
        )
    }
}
