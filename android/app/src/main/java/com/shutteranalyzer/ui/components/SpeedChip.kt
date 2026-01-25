package com.shutteranalyzer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme

/**
 * State for a shutter speed chip.
 */
enum class SpeedChipState {
    UNSELECTED,
    SELECTED,
    DETECTED  // Detected during recording (with checkmark)
}

/**
 * A selectable chip for shutter speed display.
 *
 * @param speed The shutter speed string (e.g., "1/500")
 * @param state The current state of the chip
 * @param onClick Callback when the chip is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun SpeedChip(
    speed: String,
    state: SpeedChipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = state != SpeedChipState.UNSELECTED,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state == SpeedChipState.DETECTED) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Detected",
                        modifier = Modifier.size(16.dp),
                        tint = AccuracyGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = speed,
                    fontWeight = if (state != SpeedChipState.UNSELECTED) FontWeight.Medium else FontWeight.Normal
                )
            }
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = if (state == SpeedChipState.DETECTED) {
            BorderStroke(1.dp, AccuracyGreen)
        } else {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = state == SpeedChipState.SELECTED
            )
        }
    )
}

/**
 * A non-interactive display chip showing a speed during recording.
 *
 * @param speed The shutter speed string
 * @param isActive Whether this speed is currently active/waiting
 * @param isCompleted Whether this speed has been detected
 * @param modifier Modifier for the component
 */
@Composable
fun SpeedDisplayChip(
    speed: String,
    isActive: Boolean = false,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCompleted -> AccuracyGreen.copy(alpha = 0.15f)
        isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isCompleted -> AccuracyGreen
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(14.dp),
                    tint = AccuracyGreen
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = speed,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive || isCompleted) FontWeight.Medium else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpeedChipPreview() {
    ShutterAnalyzerTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedChip(
                speed = "1/1000",
                state = SpeedChipState.UNSELECTED,
                onClick = {}
            )
            SpeedChip(
                speed = "1/500",
                state = SpeedChipState.SELECTED,
                onClick = {}
            )
            SpeedChip(
                speed = "1/250",
                state = SpeedChipState.DETECTED,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpeedDisplayChipPreview() {
    ShutterAnalyzerTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedDisplayChip(speed = "1/1000")
            SpeedDisplayChip(speed = "1/500", isActive = true)
            SpeedDisplayChip(speed = "1/250", isCompleted = true)
        }
    }
}
