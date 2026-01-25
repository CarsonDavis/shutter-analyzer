package com.shutteranalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.shutteranalyzer.data.repository.SettingsRepository
import com.shutteranalyzer.ui.navigation.NavGraph
import com.shutteranalyzer.ui.navigation.Screen
import com.shutteranalyzer.ui.theme.ShutterAnalyzerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Main activity for Shutter Analyzer.
 * Sets up the navigation and theme for the app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShutterAnalyzerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    // Determine start destination based on onboarding flag
                    LaunchedEffect(Unit) {
                        val hasSeenOnboarding = settingsRepository.hasSeenOnboarding.first()
                        startDestination = if (hasSeenOnboarding) {
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }
                    }

                    // Show navigation once we've determined the start destination
                    startDestination?.let { destination ->
                        val navController = rememberNavController()

                        // Mark onboarding as seen when navigating away from it
                        LaunchedEffect(navController) {
                            navController.addOnDestinationChangedListener { _, dest, _ ->
                                if (dest.route == Screen.Home.route) {
                                    // Mark onboarding as complete when reaching home
                                    kotlinx.coroutines.runBlocking {
                                        settingsRepository.setHasSeenOnboarding(true)
                                    }
                                }
                            }
                        }

                        NavGraph(
                            navController = navController,
                            startDestination = destination
                        )
                    }
                }
            }
        }
    }
}
