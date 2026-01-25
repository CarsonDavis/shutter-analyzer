package com.shutteranalyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import com.shutteranalyzer.ui.theme.getAccuracyColor
import com.shutteranalyzer.ui.theme.getAccuracyLabel
import kotlin.math.abs

/**
 * Displays an accuracy indicator with colored badge based on deviation percentage.
 *
 * Colors:
 * - Green: 0-5% error (Good)
 * - Yellow: 5-10% error (Fair)
 * - Orange: 10-15% error (Poor)
 * - Red: >15% error (Bad)
 *
 * @param deviationPercent The deviation percentage (absolute value will be used)
 * @param showLabel Whether to show the accuracy label (Good, Fair, etc.)
 * @param modifier Modifier for the component
 */
@Composable
fun AccuracyIndicator(
    deviationPercent: Double,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val absDeviation = abs(deviationPercent)
    val color = getAccuracyColor(absDeviation)
    val label = getAccuracyLabel(absDeviation)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${String.format("%.1f", absDeviation)}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = color
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * A simple colored dot indicator for accuracy.
 *
 * @param deviationPercent The deviation percentage
 * @param modifier Modifier for the component
 */
@Composable
fun AccuracyDot(
    deviationPercent: Double,
    modifier: Modifier = Modifier
) {
    val color = getAccuracyColor(abs(deviationPercent))
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Returns a background color for table rows based on deviation.
 */
@Composable
fun getAccuracyRowColor(deviationPercent: Double): Color {
    return getAccuracyColor(abs(deviationPercent)).copy(alpha = 0.1f)
}

@Preview(showBackground = true)
@Composable
private fun AccuracyIndicatorPreview() {
    ShutterAnalyzerTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            AccuracyIndicator(deviationPercent = 2.5)
            AccuracyIndicator(deviationPercent = 7.0)
            AccuracyIndicator(deviationPercent = 12.5)
            AccuracyIndicator(deviationPercent = 20.0)
            AccuracyIndicator(deviationPercent = 4.2, showLabel = false)
        }
    }
}
