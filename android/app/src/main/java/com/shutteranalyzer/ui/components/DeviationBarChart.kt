package com.shutteranalyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.ui.screens.results.ShutterResult
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import kotlin.math.abs

/**
 * Horizontal bar chart showing deviation for each shutter speed measurement.
 *
 * Features:
 * - Center line at 0% deviation (perfect accuracy)
 * - Bars extend left for slow (negative) or right for fast (positive)
 * - Color gradient from green (0%) through yellow/orange to red (20%+)
 * - Speed labels on the left Y-axis
 *
 * @param results List of shutter speed measurement results
 * @param modifier Modifier for the chart container
 * @param maxDeviation Maximum deviation to display (bars are clamped to this range)
 */
@Composable
fun DeviationBarChart(
    results: List<ShutterResult>,
    modifier: Modifier = Modifier,
    maxDeviation: Double = 25.0
) {
    if (results.isEmpty()) return

    val labelWidth = 60.dp
    val barHeight = 32.dp
    val barSpacing = 8.dp
    val totalHeight = (barHeight + barSpacing) * results.size

    Column(modifier = modifier.fillMaxWidth()) {
        // Chart header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = labelWidth, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Slow",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "0%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Fast",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bars with labels
        results.forEach { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight + barSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed label
                Text(
                    text = result.expectedSpeed,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Bar chart area
                DeviationBar(
                    deviation = result.deviationPercent,
                    maxDeviation = maxDeviation,
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                )

                // Deviation value
                Text(
                    text = formatDeviation(result.deviationPercent),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp),
                    color = ChartUtils.deviationToColor(result.deviationPercent)
                )
            }
        }
    }
}

/**
 * Single deviation bar that extends left or right from center.
 */
@Composable
private fun DeviationBar(
    deviation: Double,
    maxDeviation: Double,
    modifier: Modifier = Modifier
) {
    val barColor = ChartUtils.deviationToGradientColor(deviation)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val centerLineColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val barPadding = 4f
        val barThickness = height - barPadding * 2
        val cornerRadius = 4f

        // Draw background grid lines at -20%, -10%, 0%, +10%, +20%
        listOf(-20.0, -10.0, 0.0, 10.0, 20.0).forEach { percent ->
            val x = centerX + (percent / maxDeviation * centerX).toFloat()
            val lineColor = if (percent == 0.0) centerLineColor else gridColor
            val lineWidth = if (percent == 0.0) 2f else 1f
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = lineWidth
            )
        }

        // Calculate bar dimensions
        val normalizedDeviation = (deviation / maxDeviation).coerceIn(-1.0, 1.0).toFloat()
        val barWidth = abs(normalizedDeviation) * centerX

        if (barWidth > 0) {
            val barLeft = if (deviation >= 0) centerX else centerX - barWidth
            val barRight = if (deviation >= 0) centerX + barWidth else centerX

            // Draw the deviation bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(barLeft, barPadding),
                size = Size(barRight - barLeft, barThickness),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

private fun formatDeviation(percent: Double): String {
    val sign = if (percent >= 0) "+" else ""
    return "$sign${String.format("%.0f", percent)}%"
}

@Preview(showBackground = true)
@Composable
private fun DeviationBarChartPreview() {
    ShutterAnalyzerTheme {
        DeviationBarChart(
            results = listOf(
                ShutterResult("1/1000", 1.0, 0.98, -2.0),
                ShutterResult("1/500", 2.0, 2.16, 8.0),
                ShutterResult("1/250", 4.0, 4.04, 1.0),
                ShutterResult("1/125", 8.0, 8.8, 10.0),
                ShutterResult("1/60", 16.67, 19.0, 14.0),
                ShutterResult("1/30", 33.33, 40.0, 20.0)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviationBarChartAccuratePreview() {
    ShutterAnalyzerTheme {
        DeviationBarChart(
            results = listOf(
                ShutterResult("1/500", 2.0, 2.02, 1.0),
                ShutterResult("1/250", 4.0, 3.96, -1.0),
                ShutterResult("1/125", 8.0, 8.12, 1.5)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
