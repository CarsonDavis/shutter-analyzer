package com.shutteranalyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import kotlin.math.max

/**
 * Data class representing a detected shutter event region.
 *
 * @param startIndex Frame index where event starts
 * @param endIndex Frame index where event ends
 * @param label Optional label for the event (e.g., "1/500")
 */
data class EventRegion(
    val startIndex: Int,
    val endIndex: Int,
    val label: String = ""
)

/**
 * Line chart showing brightness values over time with detected events highlighted.
 *
 * Features:
 * - Line plot of brightness values (Y: 0-255, X: frame index)
 * - Horizontal dashed line at detection threshold
 * - Green shaded regions for detected events
 * - Event labels above each region
 * - Horizontal scrolling for long recordings
 *
 * @param brightnessValues List of brightness values (one per frame)
 * @param events List of detected event regions
 * @param threshold Detection threshold value
 * @param baseline Baseline brightness value (optional)
 * @param modifier Modifier for the chart container
 * @param chartHeight Height of the chart
 * @param pointsPerDp Density of data points (lower = wider chart for long recordings)
 */
@Composable
fun BrightnessTimelineChart(
    brightnessValues: List<Double>,
    events: List<EventRegion>,
    threshold: Double,
    baseline: Double = 0.0,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
    pointsPerDp: Float = 2f
) {
    if (brightnessValues.isEmpty()) return

    val scrollState = rememberScrollState()
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val thresholdColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val baselineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    val eventColor = AccuracyGreen.copy(alpha = 0.2f)
    val eventBorderColor = AccuracyGreen
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Calculate chart width based on data length
    val chartWidthDp = remember(brightnessValues.size) {
        max(brightnessValues.size / pointsPerDp, 300f).dp
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = lineColor, label = "Brightness")
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = thresholdColor, label = "Threshold", isDashed = true)
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(color = eventColor, label = "Events")
        }

        // Scrollable chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidthDp)
                    .height(chartHeight)
                    .padding(start = 40.dp, end = 8.dp, top = 16.dp, bottom = 24.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val maxBrightness = 255.0

                // Draw Y-axis labels
                drawYAxisLabels(
                    maxValue = maxBrightness,
                    chartHeight = chartHeight,
                    textColor = textColor
                )

                // Draw horizontal grid lines
                ChartUtils.run {
                    drawHorizontalGridLines(
                        gridCount = 4,
                        color = gridColor,
                        startX = 0f,
                        endX = chartWidth,
                        startY = 0f,
                        height = chartHeight
                    )
                }

                // Draw event regions (shaded background)
                events.forEach { event ->
                    drawEventRegion(
                        event = event,
                        totalPoints = brightnessValues.size,
                        chartWidth = chartWidth,
                        chartHeight = chartHeight,
                        fillColor = eventColor,
                        borderColor = eventBorderColor,
                        textColor = textColor
                    )
                }

                // Draw baseline line
                if (baseline > 0) {
                    val baselineY = chartHeight - (baseline / maxBrightness * chartHeight).toFloat()
                    ChartUtils.run {
                        drawDashedHorizontalLine(
                            y = baselineY,
                            startX = 0f,
                            endX = chartWidth,
                            color = baselineColor
                        )
                    }
                }

                // Draw threshold line
                val thresholdY = chartHeight - (threshold / maxBrightness * chartHeight).toFloat()
                ChartUtils.run {
                    drawDashedHorizontalLine(
                        y = thresholdY,
                        startX = 0f,
                        endX = chartWidth,
                        color = thresholdColor
                    )
                }

                // Draw brightness line
                drawBrightnessLine(
                    values = brightnessValues,
                    maxValue = maxBrightness,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    color = lineColor
                )

                // Draw axis border
                drawLine(
                    color = gridColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(chartWidth, chartHeight),
                    strokeWidth = 2f
                )
            }
        }

        // X-axis label
        Text(
            text = "Frame Index",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(20.dp).height(12.dp)) {
            if (isDashed) {
                ChartUtils.run {
                    drawDashedHorizontalLine(
                        y = size.height / 2,
                        startX = 0f,
                        endX = size.width,
                        color = color
                    )
                }
            } else {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 3f
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawYAxisLabels(
    maxValue: Double,
    chartHeight: Float,
    textColor: Color
) {
    val labels = listOf(0, 64, 128, 192, 255)
    labels.forEach { value ->
        val y = chartHeight - (value / maxValue * chartHeight).toFloat()
        ChartUtils.run {
            drawAxisLabel(
                text = value.toString(),
                x = -8f,
                y = y + 8f,
                textColor = textColor,
                textSize = 24f,
                centerHorizontally = false
            )
        }
    }
}

private fun DrawScope.drawEventRegion(
    event: EventRegion,
    totalPoints: Int,
    chartWidth: Float,
    chartHeight: Float,
    fillColor: Color,
    borderColor: Color,
    textColor: Color
) {
    val startX = (event.startIndex.toFloat() / totalPoints) * chartWidth
    val endX = (event.endIndex.toFloat() / totalPoints) * chartWidth
    val width = endX - startX

    // Shaded region
    drawRect(
        color = fillColor,
        topLeft = Offset(startX, 0f),
        size = Size(width, chartHeight)
    )

    // Border lines
    drawLine(
        color = borderColor,
        start = Offset(startX, 0f),
        end = Offset(startX, chartHeight),
        strokeWidth = 2f
    )
    drawLine(
        color = borderColor,
        start = Offset(endX, 0f),
        end = Offset(endX, chartHeight),
        strokeWidth = 2f
    )

    // Event label
    if (event.label.isNotEmpty()) {
        ChartUtils.run {
            drawAxisLabel(
                text = event.label,
                x = startX + width / 2,
                y = -4f,
                textColor = textColor,
                textSize = 22f,
                centerHorizontally = true
            )
        }
    }
}

private fun DrawScope.drawBrightnessLine(
    values: List<Double>,
    maxValue: Double,
    chartWidth: Float,
    chartHeight: Float,
    color: Color
) {
    if (values.size < 2) return

    val path = Path()
    val pointSpacing = chartWidth / (values.size - 1)

    values.forEachIndexed { index, value ->
        val x = index * pointSpacing
        val y = chartHeight - (value / maxValue * chartHeight).toFloat()

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2f)
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun BrightnessTimelineChartPreview() {
    // Generate sample data with events
    val baseValue = 30.0
    val peakValue = 180.0
    val values = buildList {
        // Baseline before first event
        repeat(20) { add(baseValue + (Math.random() * 5 - 2.5)) }
        // First event (1/500)
        repeat(15) { add(peakValue + (Math.random() * 10 - 5)) }
        // Baseline
        repeat(30) { add(baseValue + (Math.random() * 5 - 2.5)) }
        // Second event (1/250)
        repeat(25) { add(peakValue + (Math.random() * 10 - 5)) }
        // Baseline
        repeat(40) { add(baseValue + (Math.random() * 5 - 2.5)) }
        // Third event (1/125)
        repeat(45) { add(peakValue + (Math.random() * 10 - 5)) }
        // Baseline after
        repeat(25) { add(baseValue + (Math.random() * 5 - 2.5)) }
    }

    val events = listOf(
        EventRegion(20, 35, "1/500"),
        EventRegion(65, 90, "1/250"),
        EventRegion(130, 175, "1/125")
    )

    ShutterAnalyzerTheme {
        BrightnessTimelineChart(
            brightnessValues = values,
            events = events,
            threshold = 80.0,
            baseline = baseValue,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun BrightnessTimelineChartEmptyPreview() {
    ShutterAnalyzerTheme {
        BrightnessTimelineChart(
            brightnessValues = List(100) { 30.0 + Math.random() * 10 },
            events = emptyList(),
            threshold = 80.0,
            baseline = 30.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
