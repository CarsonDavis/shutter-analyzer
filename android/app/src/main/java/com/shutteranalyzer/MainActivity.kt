package com.shutteranalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.shutteranalyzer.ui.navigation.NavGraph
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Shutter Analyzer.
 * Sets up the navigation and theme for the app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShutterAnalyzerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
