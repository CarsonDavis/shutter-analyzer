package com.shutteranalyzer.ui.screens.videoimport

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.analysis.model.ShutterEvent
import com.shutteranalyzer.data.video.FrameExtractor
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.AccuracyOrange
import com.shutteranalyzer.ui.theme.AccuracyYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Frame data for preview display.
 */
private data class PreviewFrame(
    val frameNumber: Int,
    val brightness: Double,
    val weight: Double,
    val isContext: Boolean,
    val thumbnail: Bitmap? = null
)

/**
 * Maximum frames to display per event (including context frames).
 */
private const val MAX_FRAMES_PER_EVENT = 12
private const val CONTEXT_FRAMES = 2

/**
 * Event Preview screen for viewing event frames during import flow.
 *
 * @param eventIndex Index of the event to preview
 * @param importViewModel The shared ImportViewModel
 * @param frameExtractor Frame extractor for thumbnails
 * @param onBackClick Callback when back is pressed
 * @param onDeleteEvent Callback when delete event is pressed
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventPreviewScreen(
    eventIndex: Int,
    importViewModel: ImportViewModel,
    frameExtractor: FrameExtractor,
    onBackClick: () -> Unit,
    onDeleteEvent: () -> Unit
) {
    val event = importViewModel.getEvent(eventIndex)
    val videoUri = importViewModel.getVideoUri()
    val fps = importViewModel.getRecordingFps()
    val assignedSpeed = importViewModel.getAssignedSpeed(eventIndex)
    val totalEvents = importViewModel.getEventCount()

    // State for thumbnails
    var frames by remember { mutableStateOf<List<PreviewFrame>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Extract thumbnails when screen loads
    LaunchedEffect(eventIndex, event, videoUri) {
        if (event == null || videoUri == null) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        withContext(Dispatchers.IO) {
            // Build frame list
            val previewFrames = buildPreviewFrames(event, fps)

            // Extract thumbnails
            val frameNumbers = previewFrames.map { it.frameNumber }.filter { it >= 0 }
            val thumbnails = frameExtractor.extractFrames(
                videoUri = videoUri,
                frameNumbers = frameNumbers,
                fps = fps,
                thumbnailWidth = 160
            )

            // Update frames with thumbnails
            val framesWithThumbnails = previewFrames.map { frame ->
                frame.copy(thumbnail = thumbnails[frame.frameNumber])
            }

            withContext(Dispatchers.Main) {
                frames = framesWithThumbnails
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event ${eventIndex + 1} of $totalEvents") },
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
        if (event == null) {
            // Event not found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Event not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Event info card
                    EventInfoCard(
                        event = event,
                        assignedSpeed = assignedSpeed,
                        fps = fps
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Frame section header
                    Text(
                        text = "FRAMES",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Loading or frame grid
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading thumbnails...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Frame grid
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            frames.forEach { frame ->
                                PreviewFrameThumbnail(frame = frame)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend
                    PreviewLegend()
                }

                // Delete button at bottom
                Button(
                    onClick = {
                        importViewModel.removeEvent(eventIndex)
                        onDeleteEvent()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DELETE EVENT")
                }
            }
        }
    }
}

/**
 * Build preview frames for an event with context frames and sampling.
 */
private fun buildPreviewFrames(event: ShutterEvent, fps: Double): List<PreviewFrame> {
    val baseline = event.baselineBrightness ?: event.brightnessValues.minOrNull() ?: 0.0
    val peak = event.peakBrightness ?: event.brightnessValues.maxOrNull() ?: 255.0
    val range = peak - baseline

    val frames = mutableListOf<PreviewFrame>()

    // Context frames before (2)
    for (i in CONTEXT_FRAMES downTo 1) {
        frames.add(
            PreviewFrame(
                frameNumber = event.startFrame - i,
                brightness = baseline,
                weight = 0.0,
                isContext = true
            )
        )
    }

    // Event frames - sample if too many
    val eventFrameCount = event.brightnessValues.size
    val maxEventFrames = MAX_FRAMES_PER_EVENT - (CONTEXT_FRAMES * 2) // Reserve space for context

    val frameIndices = if (eventFrameCount <= maxEventFrames) {
        (0 until eventFrameCount).toList()
    } else {
        // Sample evenly
        val step = eventFrameCount.toDouble() / (maxEventFrames - 1)
        (0 until maxEventFrames).map { i ->
            (i * step).toInt().coerceAtMost(eventFrameCount - 1)
        }.distinct()
    }

    frameIndices.forEach { frameIdx ->
        val brightness = event.brightnessValues[frameIdx]
        val weight = if (range > 0) {
            ((brightness - baseline) / range).coerceIn(0.0, 1.0)
        } else {
            1.0
        }

        frames.add(
            PreviewFrame(
                frameNumber = event.startFrame + frameIdx,
                brightness = brightness,
                weight = weight,
                isContext = false
            )
        )
    }

    // Context frames after (2)
    for (i in 1..CONTEXT_FRAMES) {
        frames.add(
            PreviewFrame(
                frameNumber = event.endFrame + i,
                brightness = baseline,
                weight = 0.0,
                isContext = true
            )
        )
    }

    return frames
}

@Composable
private fun EventInfoCard(
    event: ShutterEvent,
    assignedSpeed: String,
    fps: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "EVENT DETAILS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Assigned Speed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = assignedSpeed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Frame Count",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${event.durationFrames} frames",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.1f ms", (event.durationFrames / fps) * 1000),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Frame Range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${event.startFrame} - ${event.endFrame}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PreviewFrameThumbnail(frame: PreviewFrame) {
    // Color based on weight (closed/partial/full)
    val borderColor = when {
        frame.isContext -> MaterialTheme.colorScheme.error  // Closed (context)
        frame.weight >= 0.95 -> AccuracyGreen  // Full
        frame.weight >= 0.5 -> AccuracyOrange  // Partial
        else -> AccuracyYellow  // Transitioning
    }

    Card(
        modifier = Modifier
            .width(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thumbnail image or placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (frame.thumbnail == null) {
                            when {
                                frame.isContext -> Color.Gray.copy(alpha = 0.3f)
                                frame.weight >= 0.95 -> AccuracyGreen.copy(alpha = 0.4f)
                                frame.weight >= 0.5 -> AccuracyOrange.copy(alpha = 0.4f)
                                else -> AccuracyYellow.copy(alpha = 0.3f)
                            }
                        } else {
                            Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (frame.thumbnail != null) {
                    Image(
                        bitmap = frame.thumbnail.asImageBitmap(),
                        contentDescription = "Frame ${frame.frameNumber}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Info bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(borderColor.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Frame number
                Text(
                    text = "#${frame.frameNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Brightness value or state
                val stateLabel = if (frame.isContext) {
                    "out"
                } else {
                    "${frame.brightness.toInt()}"
                }
                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (frame.isContext) borderColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewLegend() {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    color = Color.Gray.copy(alpha = 0.5f),
                    label = "Closed"
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
