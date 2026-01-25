package com.shutteranalyzer.ui.theme

import androidx.compose.ui.graphics.Color

// Accuracy indicator colors (from wireframes)
val AccuracyGreen = Color(0xFF4CAF50)   // 0-5% error - Accurate
val AccuracyYellow = Color(0xFFFFEB3B)  // 5-10% error - Slight deviation
val AccuracyOrange = Color(0xFFFF9800)  // 10-15% error - Moderate deviation
val AccuracyRed = Color(0xFFF44336)     // >15% error - Significant deviation
val ContextBlue = Color(0xFF2196F3)     // Context frames (not in event)

// Light theme colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Custom app colors
val ShutterPrimary = Color(0xFF1565C0)      // Deep blue
val ShutterPrimaryDark = Color(0xFF0D47A1)  // Darker blue
val ShutterSecondary = Color(0xFF26A69A)    // Teal
val ShutterBackground = Color(0xFFF5F5F5)   // Light grey
val ShutterSurface = Color(0xFFFFFFFF)      // White
val ShutterOnSurface = Color(0xFF1C1B1F)    // Near black

// Dark theme custom colors
val ShutterPrimaryDarkTheme = Color(0xFF90CAF9)
val ShutterSecondaryDarkTheme = Color(0xFF80CBC4)
val ShutterBackgroundDark = Color(0xFF121212)
val ShutterSurfaceDark = Color(0xFF1E1E1E)
val ShutterOnSurfaceDark = Color(0xFFE6E1E5)

/**
 * Get the accuracy color based on deviation percentage.
 */
fun getAccuracyColor(deviationPercent: Double): Color {
    return when {
        deviationPercent <= 5.0 -> AccuracyGreen
        deviationPercent <= 10.0 -> AccuracyYellow
        deviationPercent <= 15.0 -> AccuracyOrange
        else -> AccuracyRed
    }
}

/**
 * Get accuracy label based on deviation percentage.
 */
fun getAccuracyLabel(deviationPercent: Double): String {
    return when {
        deviationPercent <= 5.0 -> "Good"
        deviationPercent <= 10.0 -> "Fair"
        deviationPercent <= 15.0 -> "Poor"
        else -> "Bad"
    }
}
