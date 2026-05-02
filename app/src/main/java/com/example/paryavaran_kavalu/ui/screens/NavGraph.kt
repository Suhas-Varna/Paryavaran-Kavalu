package com.example.paryavaran_kavalu.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paryavaran_kavalu.ui.screens.CameraScreen
import com.example.paryavaran_kavalu.ui.screens.HomeScreen
import com.example.paryavaran_kavalu.ui.screens.LeaderboardScreen
import com.example.paryavaran_kavalu.ui.screens.MapScreen
import com.example.paryavaran_kavalu.ui.screens.ReportScreen
import com.example.paryavaran_kavalu.ui.screens.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val viewModel: WasteReportViewModel = viewModel(viewModelStoreOwner = activity)

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }

        composable("home") {
            HomeScreen(
                onGetStarted = {
                    navController.navigate("map") {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable("map") {
            MapScreen(
                viewModel = viewModel,
                onReportIncident = { navController.navigate("camera") },
                onOpenLeaderboard = { navController.navigate("leaderboard") },
                onRequestCleanPhoto = { reportId ->
                    navController.navigate("clean_camera/$reportId")
                },
                onBackToHome = { navController.popBackStack() },
            )
        }

        composable("leaderboard") {
            LeaderboardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable("report") {
            ReportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate("camera") },
                onSubmitted = {
                    navController.popBackStack("map", inclusive = false)
                },
            )
        }

        composable("camera") {
            CameraScreen(
                autoOpenCamera = true,
                onBack = { navController.popBackStack() },
                onImageCaptured = { uri ->
                    viewModel.updateCapturedImageUri(uri.toString())
                    navController.navigate("report") {
                        popUpTo("camera") { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = "clean_camera/{reportId}",
            arguments = listOf(
                navArgument("reportId") { type = NavType.LongType },
            ),
        ) { entry ->
            val reportId = entry.arguments?.getLong("reportId") ?: return@composable
            CameraScreen(
                title = "Capture cleaning proof",
                sessionKey = "clean_$reportId",
                onBack = { navController.popBackStack() },
                onImageCaptured = { uri ->
                    viewModel.markReportCleaned(reportId, uri.toString()) {
                        navController.popBackStack("map", inclusive = false)
                    }
                },
            )
        }
    }
}
