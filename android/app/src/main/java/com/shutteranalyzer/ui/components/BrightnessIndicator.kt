package com.shutteranalyzer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shutteranalyzer.ui.theme.AccuracyGreen
import com.shutteranalyzer.ui.theme.AccuracyOrange
import com.shutteranalyzer.ui.theme.AccuracyYellow
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme

/**
 * Vertical bar showing current brightness level.
 * Used during recording to show real-time brightness.
 *
 * @param brightness Current brightness value (0-255)
 * @param threshold Detection threshold (if set, shows a marker)
 * @param modifier Modifier for the component
 */
@Composable
fun BrightnessIndicator(
    brightness: Double,
    threshold: Double? = null,
    modifier: Modifier = Modifier
) {
    val normalizedBrightness = (brightness / 255.0).coerceIn(0.0, 1.0)
    val animatedBrightness by animateFloatAsState(
        targetValue = normalizedBrightness.toFloat(),
        animationSpec = tween(durationMillis = 100),
        label = "brightness"
    )

    val barColor = when {
        threshold != null && brightness > threshold -> AccuracyGreen
        brightness > 150 -> AccuracyYellow
        brightness > 80 -> AccuracyOrange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Filled portion from bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedBrightness)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )

            // Threshold marker
            if (threshold != null) {
                val thresholdPosition = (threshold / 255.0).coerceIn(0.0, 1.0).toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = (thresholdPosition * 100).dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = brightness.toInt().toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Horizontal brightness bar (alternative display).
 *
 * @param brightness Current brightness value (0-255)
 * @param modifier Modifier for the component
 */
@Composable
fun HorizontalBrightnessBar(
    brightness: Double,
    modifier: Modifier = Modifier
) {
    val normalizedBrightness = (brightness / 255.0).coerceIn(0.0, 1.0)
    val animatedBrightness by animateFloatAsState(
        targetValue = normalizedBrightness.toFloat(),
        animationSpec = tween(durationMillis = 100),
        label = "brightness"
    )

    val barColor = when {
        brightness > 150 -> AccuracyGreen
        brightness > 80 -> AccuracyYellow
        else -> AccuracyOrange
    }

    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedBrightness)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrightnessIndicatorPreview() {
    ShutterAnalyzerTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrightnessIndicator(
                brightness = 180.0,
                threshold = 100.0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HorizontalBrightnessBarPreview() {
    ShutterAnalyzerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalBrightnessBar(
                brightness = 50.0,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalBrightnessBar(
                brightness = 120.0,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalBrightnessBar(
                brightness = 200.0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
