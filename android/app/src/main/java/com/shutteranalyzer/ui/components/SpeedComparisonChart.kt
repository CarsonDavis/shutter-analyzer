package com.shutteranalyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.ui.screens.results.ShutterResult
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import kotlin.math.abs

/**
 * Scatter plot comparing expected vs measured shutter speeds.
 *
 * Features:
 * - X-axis: Expected speed (log scale from 1/1000s to 1s)
 * - Y-axis: Measured speed (log scale)
 * - Diagonal line represents perfect accuracy
 * - Points colored by deviation magnitude
 * - Useful for identifying systematic bias (all points above/below diagonal)
 *
 * @param results List of shutter speed measurement results
 * @param modifier Modifier for the chart container
 */
@Composable
fun SpeedComparisonChart(
    results: List<ShutterResult>,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    // Calculate min/max for axis ranges
    val allSpeeds = results.flatMap { listOf(it.expectedMs, it.measuredMs) }
    val minSpeed = allSpeeds.minOrNull()?.coerceAtLeast(0.5) ?: 1.0
    val maxSpeed = allSpeeds.maxOrNull()?.coerceAtMost(2000.0) ?: 1000.0

    // Extend range slightly for padding
    val rangeMin = (minSpeed * 0.5).coerceAtLeast(0.5)
    val rangeMax = maxSpeed * 2.0

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val referenceLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    // Speed labels for axis
    val speedLabels = remember {
        listOf(
            1.0 to "1/1000",
            2.0 to "1/500",
            4.0 to "1/250",
            8.0 to "1/125",
            16.67 to "1/60",
            33.33 to "1/30",
            66.67 to "1/15",
            125.0 to "1/8",
            250.0 to "1/4",
            500.0 to "1/2",
            1000.0 to "1s"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(start = 48.dp, bottom = 32.dp, end = 8.dp, top = 8.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

            // Draw grid and axis labels
            drawChartGrid(
                speedLabels = speedLabels,
                rangeMin = rangeMin,
                rangeMax = rangeMax,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                gridColor = gridColor,
                textColor = textColor
            )

            // Draw perfect accuracy diagonal line
            ChartUtils.run {
                drawDashedLine(
                    start = Offset(0f, chartHeight),
                    end = Offset(chartWidth, 0f),
                    color = referenceLineColor
                )
            }

            // Draw data points
            results.forEach { result ->
                val x = ChartUtils.speedToLogPosition(
                    result.expectedMs,
                    rangeMin,
                    rangeMax
                ) * chartWidth

                val y = chartHeight - ChartUtils.speedToLogPosition(
                    result.measuredMs,
                    rangeMin,
                    rangeMax
                ) * chartHeight

                val pointColor = ChartUtils.deviationToGradientColor(result.deviationPercent)

                // Draw point with outline
                drawCircle(
                    color = pointColor,
                    radius = 12f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = pointColor,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun DrawScope.drawChartGrid(
    speedLabels: List<Pair<Double, String>>,
    rangeMin: Double,
    rangeMax: Double,
    chartWidth: Float,
    chartHeight: Float,
    gridColor: Color,
    textColor: Color
) {
    // Draw grid lines for common shutter speeds
    speedLabels.forEach { (speedMs, label) ->
        if (speedMs in rangeMin..rangeMax) {
            val pos = ChartUtils.speedToLogPosition(speedMs, rangeMin, rangeMax)

            // Vertical grid line (X-axis)
            val x = pos * chartWidth
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, chartHeight),
                strokeWidth = 1f
            )

            // X-axis label
            ChartUtils.run {
                drawAxisLabel(
                    text = label,
                    x = x,
                    y = chartHeight + 20f,
                    textColor = textColor,
                    textSize = 24f,
                    centerHorizontally = true
                )
            }

            // Horizontal grid line (Y-axis)
            val y = chartHeight - pos * chartHeight
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )

            // Y-axis label
            ChartUtils.run {
                drawAxisLabel(
                    text = label,
                    x = -8f,
                    y = y + 8f,
                    textColor = textColor,
                    textSize = 24f,
                    centerHorizontally = false
                )
            }
        }
    }

    // Draw axis borders
    drawLine(
        color = gridColor,
        start = Offset(0f, 0f),
        end = Offset(0f, chartHeight),
        strokeWidth = 2f
    )
    drawLine(
        color = gridColor,
        start = Offset(0f, chartHeight),
        end = Offset(chartWidth, chartHeight),
        strokeWidth = 2f
    )
}

@Preview(showBackground = true, widthDp = 350, heightDp = 400)
@Composable
private fun SpeedComparisonChartPreview() {
    ShutterAnalyzerTheme {
        SpeedComparisonChart(
            results = listOf(
                ShutterResult("1/500", 2.0, 2.1, 5.0),
                ShutterResult("1/250", 4.0, 4.4, 10.0),
                ShutterResult("1/125", 8.0, 8.2, 2.5),
                ShutterResult("1/60", 16.67, 18.0, 8.0),
                ShutterResult("1/30", 33.33, 35.0, 5.0)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 350, heightDp = 400)
@Composable
private fun SpeedComparisonChartAccuratePreview() {
    ShutterAnalyzerTheme {
        SpeedComparisonChart(
            results = listOf(
                ShutterResult("1/500", 2.0, 2.02, 1.0),
                ShutterResult("1/250", 4.0, 3.96, -1.0),
                ShutterResult("1/125", 8.0, 8.08, 1.0),
                ShutterResult("1/60", 16.67, 16.5, -1.0),
                ShutterResult("1/30", 33.33, 33.8, 1.4)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
