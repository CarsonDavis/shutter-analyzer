package com.shutteranalyzer.ui.screens.results

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shutteranalyzer.ui.components.AccuracyIndicator
import com.shutteranalyzer.ui.components.BrightnessTimelineChart
import com.shutteranalyzer.ui.components.DeviationBarChart
import com.shutteranalyzer.ui.components.EventRegion
import com.shutteranalyzer.ui.components.SpeedComparisonChart
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import com.shutteranalyzer.ui.theme.getAccuracyColor
import kotlin.math.abs

/**
 * Results screen displaying shutter speed measurement results.
 *
 * @param sessionId The test session ID
 * @param onBackClick Callback when back/save is clicked
 * @param onTestAgain Callback when test again is clicked
 * @param viewModel The ViewModel for this screen
 */
/**
 * Tab indices for the results screen.
 */
private object ResultsTabs {
    const val SUMMARY = 0
    const val DEVIATION = 1
    const val COMPARISON = 2
    const val TIMELINE = 3
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    sessionId: Long,
    onBackClick: () -> Unit,
    onTestAgain: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val camera by viewModel.camera.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val averageDeviation by viewModel.averageDeviation.collectAsStateWithLifecycle()
    val testDate by viewModel.testDate.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val timelineData by viewModel.timelineData.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(ResultsTabs.SUMMARY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RESULTS") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header section (always visible)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = camera?.name ?: "Quick Test",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tested: $testDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Average deviation card (always visible)
            AverageDeviationCard(
                averageDeviation = averageDeviation,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tab row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == ResultsTabs.SUMMARY,
                    onClick = { selectedTab = ResultsTabs.SUMMARY },
                    text = { Text("Summary") }
                )
                Tab(
                    selected = selectedTab == ResultsTabs.DEVIATION,
                    onClick = { selectedTab = ResultsTabs.DEVIATION },
                    text = { Text("Deviation") }
                )
                Tab(
                    selected = selectedTab == ResultsTabs.COMPARISON,
                    onClick = { selectedTab = ResultsTabs.COMPARISON },
                    text = { Text("Accuracy") }
                )
                Tab(
                    selected = selectedTab == ResultsTabs.TIMELINE,
                    onClick = { selectedTab = ResultsTabs.TIMELINE },
                    text = { Text("Timeline") }
                )
            }

            // Tab content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    ResultsTabs.SUMMARY -> {
                        // Results table
                        item {
                            Text(
                                text = "ACCURACY TABLE",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        item {
                            ResultsTableHeader()
                        }
                        items(results) { result ->
                            ResultRow(
                                result = result,
                                formatMs = viewModel::formatMs,
                                formatDeviation = viewModel::formatDeviation
                            )
                        }
                    }

                    ResultsTabs.DEVIATION -> {
                        item {
                            Text(
                                text = "DEVIATION BY SPEED",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        item {
                            Text(
                                text = "Bars show how much each shutter speed deviates from expected. Left = slow (shutter open longer), Right = fast.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            DeviationBarChart(
                                results = results,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ResultsTabs.COMPARISON -> {
                        item {
                            Text(
                                text = "EXPECTED VS MEASURED",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        item {
                            Text(
                                text = "Points on the diagonal line indicate perfect accuracy. Points above = slow shutter, below = fast shutter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            SpeedComparisonChart(
                                results = results,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ResultsTabs.TIMELINE -> {
                        item {
                            Text(
                                text = "BRIGHTNESS TIMELINE",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        item {
                            Text(
                                text = "Shows brightness over time. Green regions are detected shutter events. Dashed line is the detection threshold.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            timelineData?.let { data ->
                                BrightnessTimelineChart(
                                    brightnessValues = data.brightnessValues,
                                    events = data.events,
                                    threshold = data.threshold,
                                    baseline = data.baseline,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } ?: Text(
                                text = "No timeline data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom buttons (always visible)
            BottomButtons(
                isSaved = isSaved,
                onSave = {
                    viewModel.saveSession()
                    onBackClick()
                },
                onTestAgain = onTestAgain,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun AverageDeviationCard(
    averageDeviation: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Average Deviation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            AccuracyIndicator(
                deviationPercent = averageDeviation,
                showLabel = true
            )
        }
    }
}

@Composable
private fun ResultsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Speed",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Expected",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            text = "Actual",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            text = "Error",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ResultRow(
    result: ShutterResult,
    formatMs: (Double) -> String,
    formatDeviation: (Double) -> String
) {
    val rowColor = getAccuracyColor(abs(result.deviationPercent)).copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = result.expectedSpeed,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatMs(result.expectedMs),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatMs(result.measuredMs),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            text = formatDeviation(result.deviationPercent),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.End,
            color = getAccuracyColor(abs(result.deviationPercent))
        )
    }
    HorizontalDivider()
}

@Composable
private fun BottomButtons(
    isSaved: Boolean,
    onSave: () -> Unit,
    onTestAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = !isSaved
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isSaved) "SAVED" else "SAVE")
        }

        OutlinedButton(
            onClick = onTestAgain,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("TEST AGAIN")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AverageDeviationCardPreview() {
    ShutterAnalyzerTheme {
        AverageDeviationCard(averageDeviation = 4.2)
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultRowPreview() {
    ShutterAnalyzerTheme {
        Column {
            ResultsTableHeader()
            ResultRow(
                result = ShutterResult(
                    expectedSpeed = "1/500",
                    expectedMs = 2.0,
                    measuredMs = 2.12,
                    deviationPercent = 6.0
                ),
                formatMs = { ms -> "${String.format("%.2f", ms)}ms" },
                formatDeviation = { p -> "+${p.toInt()}%" }
            )
            ResultRow(
                result = ShutterResult(
                    expectedSpeed = "1/250",
                    expectedMs = 4.0,
                    measuredMs = 4.08,
                    deviationPercent = 2.0
                ),
                formatMs = { ms -> "${String.format("%.2f", ms)}ms" },
                formatDeviation = { p -> "+${p.toInt()}%" }
            )
        }
    }
}
