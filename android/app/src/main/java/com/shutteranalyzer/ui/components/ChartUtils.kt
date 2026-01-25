package com.shutteranalyzer.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.AccuracyOrange
import com.shutteranalyzer.ui.theme.AccuracyRed
import com.shutteranalyzer.ui.theme.AccuracyYellow
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * Shared utilities for chart drawing using Compose Canvas.
 */
object ChartUtils {

    /**
     * Draw horizontal grid lines across the chart area.
     *
     * @param gridCount Number of grid lines to draw
     * @param color Grid line color
     * @param startX Left edge of chart area
     * @param endX Right edge of chart area
     * @param startY Top edge of chart area
     * @param height Height of chart area
     */
    fun DrawScope.drawHorizontalGridLines(
        gridCount: Int,
        color: Color,
        startX: Float,
        endX: Float,
        startY: Float,
        height: Float
    ) {
        val spacing = height / (gridCount + 1)
        for (i in 1..gridCount) {
            val y = startY + spacing * i
            drawLine(
                color = color.copy(alpha = 0.2f),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 1f
            )
        }
    }

    /**
     * Draw vertical grid lines across the chart area.
     *
     * @param gridCount Number of grid lines to draw
     * @param color Grid line color
     * @param startX Left edge of chart area
     * @param width Width of chart area
     * @param startY Top edge of chart area
     * @param endY Bottom edge of chart area
     */
    fun DrawScope.drawVerticalGridLines(
        gridCount: Int,
        color: Color,
        startX: Float,
        width: Float,
        startY: Float,
        endY: Float
    ) {
        val spacing = width / (gridCount + 1)
        for (i in 1..gridCount) {
            val x = startX + spacing * i
            drawLine(
                color = color.copy(alpha = 0.2f),
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = 1f
            )
        }
    }

    /**
     * Draw a dashed horizontal line (e.g., for threshold indicator).
     *
     * @param y Y position
     * @param startX Left edge
     * @param endX Right edge
     * @param color Line color
     */
    fun DrawScope.drawDashedHorizontalLine(
        y: Float,
        startX: Float,
        endX: Float,
        color: Color
    ) {
        drawLine(
            color = color,
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )
    }

    /**
     * Draw a dashed diagonal line (e.g., for perfect accuracy reference).
     *
     * @param start Start point
     * @param end End point
     * @param color Line color
     */
    fun DrawScope.drawDashedLine(
        start: Offset,
        end: Offset,
        color: Color
    ) {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
        )
    }

    /**
     * Get color based on deviation percentage using the app's accuracy color scheme.
     *
     * @param deviationPercent Absolute deviation percentage
     * @return Color from green (0%) through yellow/orange to red (20%+)
     */
    fun deviationToColor(deviationPercent: Double): Color {
        val absDeviation = abs(deviationPercent)
        return when {
            absDeviation <= 5.0 -> AccuracyGreen
            absDeviation <= 10.0 -> AccuracyYellow
            absDeviation <= 15.0 -> AccuracyOrange
            else -> AccuracyRed
        }
    }

    /**
     * Get interpolated color between accuracy colors based on deviation.
     *
     * @param deviationPercent Absolute deviation percentage
     * @return Smoothly interpolated color
     */
    fun deviationToGradientColor(deviationPercent: Double): Color {
        val absDeviation = abs(deviationPercent)
        return when {
            absDeviation <= 5.0 -> {
                // Green zone
                AccuracyGreen
            }
            absDeviation <= 10.0 -> {
                // Interpolate green to yellow
                val t = ((absDeviation - 5.0) / 5.0).toFloat()
                lerpColor(AccuracyGreen, AccuracyYellow, t)
            }
            absDeviation <= 15.0 -> {
                // Interpolate yellow to orange
                val t = ((absDeviation - 10.0) / 5.0).toFloat()
                lerpColor(AccuracyYellow, AccuracyOrange, t)
            }
            absDeviation <= 20.0 -> {
                // Interpolate orange to red
                val t = ((absDeviation - 15.0) / 5.0).toFloat()
                lerpColor(AccuracyOrange, AccuracyRed, t)
            }
            else -> AccuracyRed
        }
    }

    /**
     * Linear interpolation between two colors.
     */
    private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        val t = fraction.coerceIn(0f, 1f)
        return Color(
            red = start.red + (end.red - start.red) * t,
            green = start.green + (end.green - start.green) * t,
            blue = start.blue + (end.blue - start.blue) * t,
            alpha = start.alpha + (end.alpha - start.alpha) * t
        )
    }

    /**
     * Convert a shutter speed value to log scale position.
     * Useful for displaying shutter speeds from 1/1000s to 1s on a linear axis.
     *
     * @param speedMs Shutter speed in milliseconds
     * @param minMs Minimum speed (fastest, e.g., 1ms = 1/1000s)
     * @param maxMs Maximum speed (slowest, e.g., 1000ms = 1s)
     * @return Position in range 0.0 to 1.0
     */
    fun speedToLogPosition(speedMs: Double, minMs: Double = 1.0, maxMs: Double = 1000.0): Float {
        if (speedMs <= 0) return 0f
        val logMin = log10(minMs)
        val logMax = log10(maxMs)
        val logSpeed = log10(speedMs.coerceIn(minMs, maxMs))
        return ((logSpeed - logMin) / (logMax - logMin)).toFloat()
    }

    /**
     * Convert a log scale position back to shutter speed.
     *
     * @param position Position in range 0.0 to 1.0
     * @param minMs Minimum speed in ms
     * @param maxMs Maximum speed in ms
     * @return Shutter speed in milliseconds
     */
    fun logPositionToSpeed(position: Float, minMs: Double = 1.0, maxMs: Double = 1000.0): Double {
        val logMin = log10(minMs)
        val logMax = log10(maxMs)
        val logSpeed = logMin + (logMax - logMin) * position
        return 10.0.pow(logSpeed)
    }

    /**
     * Format a shutter speed for display.
     *
     * @param speedMs Speed in milliseconds
     * @return Formatted string like "1/500" or "1s"
     */
    fun formatSpeed(speedMs: Double): String {
        return when {
            speedMs >= 1000 -> "${(speedMs / 1000).toInt()}s"
            speedMs >= 500 -> "1/2"
            speedMs >= 250 -> "1/4"
            speedMs >= 125 -> "1/8"
            speedMs >= 62 -> "1/15"
            speedMs >= 31 -> "1/30"
            speedMs >= 16 -> "1/60"
            speedMs >= 8 -> "1/125"
            speedMs >= 4 -> "1/250"
            speedMs >= 2 -> "1/500"
            else -> "1/1000"
        }
    }

    /**
     * Draw text on canvas using native Android canvas.
     *
     * @param text Text to draw
     * @param x X position
     * @param y Y position
     * @param textColor Text color
     * @param textSize Text size in pixels
     * @param centerHorizontally Whether to center text horizontally at x
     */
    fun DrawScope.drawAxisLabel(
        text: String,
        x: Float,
        y: Float,
        textColor: Color,
        textSize: Float,
        centerHorizontally: Boolean = false
    ) {
        val paint = android.graphics.Paint().apply {
            color = textColor.toArgb()
            this.textSize = textSize
            isAntiAlias = true
            if (centerHorizontally) {
                textAlign = android.graphics.Paint.Align.CENTER
            }
        }
        drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
    }

    /**
     * Convert Compose Color to ARGB int for native canvas.
     */
    private fun Color.toArgb(): Int {
        return android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )
    }
}
