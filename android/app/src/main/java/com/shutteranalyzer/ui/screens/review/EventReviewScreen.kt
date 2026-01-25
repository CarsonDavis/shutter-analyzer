package com.shutteranalyzer.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.AccuracyOrange
import com.shutteranalyzer.ui.theme.AccuracyYellow
import com.shutteranalyzer.ui.theme.ContextBlue
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import kotlinx.coroutines.launch

/**
 * Event review screen for reviewing and adjusting detected events.
 *
 * @param sessionId The test session ID
 * @param onBackClick Callback when back button is clicked
 * @param onConfirmAndCalculate Callback when confirm is clicked
 * @param viewModel The ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventReviewScreen(
    sessionId: Long,
    onBackClick: () -> Unit,
    onConfirmAndCalculate: () -> Unit,
    viewModel: EventReviewViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val currentEventIndex by viewModel.currentEventIndex.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val currentEvent = if (events.isNotEmpty() && currentEventIndex in events.indices) {
        events[currentEventIndex]
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("REVIEW EVENTS")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (events.isNotEmpty()) {
                        Text(
                            text = "${currentEventIndex + 1} of ${events.size}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events to review",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    currentEvent?.let { event ->
                        EventReviewContent(
                            event = event,
                            onToggleFrame = { frameIndex ->
                                viewModel.toggleFrameInclusion(frameIndex)
                            }
                        )
                    }
                }

                // Navigation and confirm buttons
                BottomControls(
                    currentIndex = currentEventIndex,
                    totalEvents = events.size,
                    onPrevious = viewModel::previousEvent,
                    onNext = viewModel::nextEvent,
                    onConfirm = {
                        scope.launch {
                            viewModel.confirmAndCalculate()
                            onConfirmAndCalculate()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventReviewContent(
    event: ReviewEvent,
    onToggleFrame: (Int) -> Unit
) {
    Column {
        // Event header
        Text(
            text = "EVENT ${event.index + 1}: ${event.expectedSpeed}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap frames to adjust boundary",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Frame grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            event.frames.forEachIndexed { index, frame ->
                FrameThumbnail(
                    frame = frame,
                    onClick = { if (!frame.isContext) onToggleFrame(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Legend
        Legend()
    }
}

@Composable
private fun FrameThumbnail(
    frame: FrameInfo,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        frame.isContext -> ContextBlue.copy(alpha = 0.3f)
        frame.weight >= 0.95 -> AccuracyGreen.copy(alpha = 0.6f)
        frame.weight >= 0.5 -> AccuracyOrange.copy(alpha = 0.6f)
        else -> AccuracyYellow.copy(alpha = 0.4f)
    }

    val borderColor = when {
        frame.isContext -> ContextBlue
        frame.isIncluded -> Color.Transparent
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .size(72.dp)
            .then(
                if (!frame.isContext) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .border(
                width = if (!frame.isIncluded && !frame.isContext) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brightness value
            Text(
                text = frame.brightness.toInt().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (frame.isContext) ContextBlue else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Status indicator
            if (frame.isContext) {
                Text(
                    text = "ctx",
                    style = MaterialTheme.typography.labelSmall,
                    color = ContextBlue
                )
            } else if (frame.isIncluded) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Included",
                    modifier = Modifier.size(16.dp),
                    tint = AccuracyGreen
                )
            } else {
                Text(
                    text = "excluded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Legend:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(
                    color = AccuracyGreen.copy(alpha = 0.6f),
                    label = "Full"
                )
                LegendItem(
                    color = AccuracyOrange.copy(alpha = 0.6f),
                    label = "Partial"
                )
                LegendItem(
                    color = ContextBlue.copy(alpha = 0.3f),
                    label = "Context"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun BottomControls(
    currentIndex: Int,
    totalEvents: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = currentIndex > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "Previous"
                )
            }

            Text(
                text = "${currentIndex + 1} / $totalEvents",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            IconButton(
                onClick = onNext,
                enabled = currentIndex < totalEvents - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Next"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CONFIRM & CALCULATE")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FrameThumbnailPreview() {
    ShutterAnalyzerTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrameThumbnail(
                frame = FrameInfo(100, 15.0, false, 0.0, isContext = true),
                onClick = {}
            )
            FrameThumbnail(
                frame = FrameInfo(101, 180.0, true, 0.8, isContext = false),
                onClick = {}
            )
            FrameThumbnail(
                frame = FrameInfo(102, 240.0, true, 1.0, isContext = false),
                onClick = {}
            )
            FrameThumbnail(
                frame = FrameInfo(103, 95.0, false, 0.4, isContext = false),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LegendPreview() {
    ShutterAnalyzerTheme {
        Legend()
    }
}
