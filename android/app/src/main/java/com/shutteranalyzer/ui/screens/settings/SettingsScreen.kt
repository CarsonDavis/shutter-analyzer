package com.shutteranalyzer.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shutteranalyzer.data.local.datastore.AppSettings
import com.shutteranalyzer.data.local.datastore.Sensitivity
import com.shutteranalyzer.data.local.datastore.SpeedSet
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme

/**
 * Settings screen composable.
 *
 * @param onBackClick Callback when back button is clicked
 * @param onViewTutorial Callback when "View Tutorial" is clicked
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onViewTutorial: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        SettingsContent(
            settings = settings,
            onSpeedSetChange = viewModel::setDefaultSpeedSet,
            onSensitivityChange = viewModel::setDetectionSensitivity,
            onViewTutorial = {
                viewModel.resetOnboarding()
                onViewTutorial()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onSpeedSetChange: (SpeedSet) -> Unit,
    onSensitivityChange: (Sensitivity) -> Unit,
    onViewTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recording Section
        SettingsSection(title = "Recording") {
            SpeedSetSelector(
                selectedSpeedSet = settings.defaultSpeedSet,
                onSpeedSetChange = onSpeedSetChange
            )
        }

        // Detection Section
        SettingsSection(title = "Detection") {
            SensitivitySelector(
                selectedSensitivity = settings.detectionSensitivity,
                onSensitivityChange = onSensitivityChange
            )
        }

        // Help Section
        SettingsSection(title = "Help") {
            ListItem(
                headlineContent = { Text("View Tutorial") },
                supportingContent = { Text("Learn how to use the app") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onViewTutorial)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("How It Works") },
                supportingContent = { Text("Shutter speed measurement theory") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable { /* TODO: Open theory page */ }
            )
        }

        // About Section
        SettingsSection(title = "About") {
            ListItem(
                headlineContent = { Text("Shutter Analyzer") },
                supportingContent = { Text("Version 1.0") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Copyright") },
                supportingContent = { Text("2025") }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SpeedSetSelector(
    selectedSpeedSet: SpeedSet,
    onSpeedSetChange: (SpeedSet) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        SpeedSet.entries.filter { it != SpeedSet.CUSTOM }.forEach { speedSet ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedSpeedSet == speedSet,
                        onClick = { onSpeedSetChange(speedSet) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSpeedSet == speedSet,
                    onClick = null
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = speedSet.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (speedSet.speeds.isNotEmpty()) {
                        Text(
                            text = speedSet.speeds.take(5).joinToString(", ") +
                                    if (speedSet.speeds.size > 5) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (speedSet != SpeedSet.entries.filter { it != SpeedSet.CUSTOM }.last()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun SensitivitySelector(
    selectedSensitivity: Sensitivity,
    onSensitivityChange: (Sensitivity) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Detection Sensitivity",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Higher sensitivity detects fainter shutter openings but may cause false positives",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Sensitivity slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Sensitivity.entries.forEach { sensitivity ->
                Text(
                    text = sensitivity.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selectedSensitivity == sensitivity)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = selectedSensitivity.value.toFloat(),
            onValueChange = { value ->
                val newSensitivity = Sensitivity.entries.find { it.value == value.toInt() }
                if (newSensitivity != null) {
                    onSensitivityChange(newSensitivity)
                }
            },
            valueRange = 0f..2f,
            steps = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ShutterAnalyzerTheme {
        SettingsContent(
            settings = AppSettings(),
            onSpeedSetChange = {},
            onSensitivityChange = {},
            onViewTutorial = {}
        )
    }
}
