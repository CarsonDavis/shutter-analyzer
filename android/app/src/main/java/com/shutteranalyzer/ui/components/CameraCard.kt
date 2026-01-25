package com.shutteranalyzer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.domain.model.Camera
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Card displaying camera information for the camera list.
 *
 * @param camera The camera to display
 * @param lastTestedAt The last test date (optional)
 * @param avgDeviation Average deviation from last test (optional)
 * @param onClick Callback when the card is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun CameraCard(
    camera: Camera,
    lastTestedAt: Instant? = null,
    avgDeviation: Double? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = camera.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (lastTestedAt != null) {
                    Text(
                        text = "Last tested: ${formatDate(lastTestedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (camera.testCount > 0) {
                    Text(
                        text = "${camera.testCount} test${if (camera.testCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (avgDeviation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Avg deviation: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AccuracyIndicator(
                            deviationPercent = avgDeviation,
                            showLabel = true
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@Preview(showBackground = true)
@Composable
private fun CameraCardPreview() {
    ShutterAnalyzerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CameraCard(
                camera = Camera(
                    id = 1,
                    name = "Fuji GW690II",
                    createdAt = Instant.now(),
                    testCount = 3
                ),
                lastTestedAt = Instant.now(),
                avgDeviation = 4.2,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            CameraCard(
                camera = Camera(
                    id = 2,
                    name = "Nikon F3",
                    createdAt = Instant.now(),
                    testCount = 1
                ),
                onClick = {}
            )
        }
    }
}
