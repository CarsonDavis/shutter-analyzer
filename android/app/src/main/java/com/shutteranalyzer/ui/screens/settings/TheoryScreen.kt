package com.shutteranalyzer.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Theory screen displaying shutter speed measurement theory.
 *
 * @param onBackClick Callback when back button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheoryScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How It Works") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TheorySection(
                title = "What is Shutter Speed?",
                content = """
                    Shutter speed is the length of time a camera's shutter remains open during an exposure. It's typically expressed as a fraction of a second:

                    • 1/500 second (0.002s) - Fast, freezes motion
                    • 1/60 second (0.0167s) - Standard
                    • 1/4 second (0.25s) - Slow, allows motion blur
                """.trimIndent()
            )

            TheorySection(
                title = "Why Measure Shutter Speed?",
                content = """
                    Vintage film cameras with mechanical shutters can drift from their calibrated speeds over time due to:

                    • Aging lubricants
                    • Spring fatigue
                    • Debris or corrosion
                    • Temperature sensitivity

                    Accurate shutter speeds are essential for proper exposure. A shutter running slow will overexpose film; one running fast will underexpose.
                """.trimIndent()
            )

            TheorySection(
                title = "High-Speed Video Capture",
                content = """
                    Modern smartphones can record slow-motion video at high frame rates (120fps, 240fps, or higher).

                    Each frame represents a fixed slice of time. At 240fps, each frame captures 1/240 second (4.17ms) of real time.

                    When recording a camera shutter:
                    1. Position the phone to see the back of the shutter (aperture fully open)
                    2. Place a bright light source behind the shutter
                    3. When the shutter opens, bright light is visible
                    4. When closed, the frame is dark
                """.trimIndent()
            )

            TheorySection(
                title = "The Calculation",
                content = """
                    shutter_duration = frame_count / recording_fps
                    shutter_speed = 1 / shutter_duration

                    Example: Recording at 240fps, shutter open for 3 frames:
                    duration = 3 / 240 = 0.0125 seconds
                    speed = 1 / 0.0125 = 80 → approximately 1/80 second
                """.trimIndent()
            )

            TheorySection(
                title = "Weighted Frame Counting",
                content = """
                    Simple frame counting treats each frame as either "open" or "closed". This creates quantization error.

                    Instead, we weight each frame by how "open" the shutter appears:

                    weight = (brightness - baseline) / (peak - baseline)

                    • Frame at baseline brightness → weight = 0 (closed)
                    • Frame at peak brightness → weight = 1 (fully open)
                    • Frame between → proportional weight (transitioning)

                    This provides sub-frame accuracy.
                """.trimIndent()
            )

            TheorySection(
                title = "Two-Phase Calibration",
                content = """
                    The app uses a two-phase calibration process:

                    Phase 1 - Baseline:
                    Collect 60 dark frames (shutter closed) to establish baseline brightness.

                    Phase 2 - Calibration Shutter:
                    User fires shutter once to calibrate. This event is discarded and not counted toward measurements. The app captures peak brightness and calculates the final detection threshold.

                    threshold = baseline + (peak - baseline) × 0.8
                """.trimIndent()
            )

            TheorySection(
                title = "Accuracy Limits",
                content = """
                    Recording fps limits measurable speeds:

                    • 240fps → Accurate for 1/250 and slower
                    • 120fps → Accurate for 1/125 and slower
                    • 60fps → Not recommended for fast speeds

                    Rule of thumb: Recording fps should be at least 2x the shutter speed being measured.

                    For vintage cameras, accuracy within 10-20% of marked speed is often acceptable.
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TheorySection(
    title: String,
    content: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(24.dp))
}
