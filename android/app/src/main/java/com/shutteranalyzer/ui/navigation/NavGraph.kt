package com.shutteranalyzer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shutteranalyzer.ui.screens.camera.CameraDetailScreen
import com.shutteranalyzer.ui.screens.home.HomeScreen
import com.shutteranalyzer.ui.screens.videoimport.ImportScreen
import com.shutteranalyzer.ui.screens.onboarding.OnboardingScreen
import com.shutteranalyzer.ui.screens.recording.RecordingScreen
import com.shutteranalyzer.ui.screens.results.ResultsScreen
import com.shutteranalyzer.ui.screens.review.EventReviewScreen
import com.shutteranalyzer.ui.screens.settings.SettingsScreen
import com.shutteranalyzer.ui.screens.setup.RecordingSetupScreen

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object RecordingSetup : Screen("setup")
    object Recording : Screen("recording/{sessionId}") {
        fun createRoute(sessionId: Long) = "recording/$sessionId"
    }
    object EventReview : Screen("review/{sessionId}") {
        fun createRoute(sessionId: Long) = "review/$sessionId"
    }
    object Results : Screen("results/{sessionId}") {
        fun createRoute(sessionId: Long) = "results/$sessionId"
    }
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
    object CameraDetail : Screen("camera/{cameraId}") {
        fun createRoute(cameraId: Long) = "camera/$cameraId"
    }
    object Import : Screen("import")
}

/**
 * Navigation graph composable.
 *
 * @param navController The navigation controller
 * @param startDestination The starting destination (default: Home)
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home screen
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNewTestClick = {
                    navController.navigate(Screen.RecordingSetup.route)
                },
                onCameraClick = { cameraId ->
                    navController.navigate(Screen.CameraDetail.createRoute(cameraId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onImportClick = {
                    navController.navigate(Screen.Import.route)
                }
            )
        }

        // Recording setup screen
        composable(route = Screen.RecordingSetup.route) {
            RecordingSetupScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onStartRecording = { sessionId ->
                    navController.navigate(Screen.Recording.createRoute(sessionId)) {
                        // Remove setup from backstack so user can't go back to it during recording
                        popUpTo(Screen.RecordingSetup.route) { inclusive = true }
                    }
                }
            )
        }

        // Recording screen
        composable(
            route = Screen.Recording.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            RecordingScreen(
                sessionId = sessionId,
                onRecordingComplete = {
                    navController.navigate(Screen.EventReview.createRoute(sessionId)) {
                        popUpTo(Screen.Recording.route) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Event review screen
        composable(
            route = Screen.EventReview.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            EventReviewScreen(
                sessionId = sessionId,
                onBackClick = {
                    navController.popBackStack()
                },
                onConfirmAndCalculate = {
                    navController.navigate(Screen.Results.createRoute(sessionId)) {
                        popUpTo(Screen.EventReview.route) { inclusive = true }
                    }
                }
            )
        }

        // Results screen
        composable(
            route = Screen.Results.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            ResultsScreen(
                sessionId = sessionId,
                onBackClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onTestAgain = {
                    navController.navigate(Screen.RecordingSetup.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Settings screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onViewTutorial = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }

        // Camera detail screen
        composable(
            route = Screen.CameraDetail.route,
            arguments = listOf(navArgument("cameraId") { type = NavType.LongType })
        ) { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getLong("cameraId") ?: return@composable
            CameraDetailScreen(
                cameraId = cameraId,
                onBackClick = {
                    navController.popBackStack()
                },
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.Results.createRoute(sessionId))
                },
                onTestAgain = {
                    navController.navigate(Screen.RecordingSetup.route)
                }
            )
        }

        // Onboarding screen
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Import screen
        composable(route = Screen.Import.route) {
            ImportScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onComplete = { sessionId ->
                    navController.navigate(Screen.Results.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
    }
}
