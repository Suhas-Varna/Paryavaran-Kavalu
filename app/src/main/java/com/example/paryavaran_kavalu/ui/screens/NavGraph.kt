package com.example.paryavaran_kavalu.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paryavaran_kavalu.ui.EcoKarma
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.example.paryavaran_kavalu.ui.screens.CameraScreen
import com.example.paryavaran_kavalu.ui.screens.CleanupSuccessScreen
import com.example.paryavaran_kavalu.ui.screens.HomeScreen
import com.example.paryavaran_kavalu.ui.screens.LeaderboardScreen
import com.example.paryavaran_kavalu.ui.screens.MapScreen
import com.example.paryavaran_kavalu.ui.screens.ProfileScreen
import com.example.paryavaran_kavalu.ui.screens.ReportDetailScreen
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
                viewModel = viewModel,
                onGetStarted = {
                    navController.navigate("map") {
                        launchSingleTop = true
                    }
                },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard") {
                        launchSingleTop = true
                    }
                },
                onOpenProfile = {
                    navController.navigate("profile") {
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
                onOpenProfile = { navController.navigate("profile") },
                onRequestCleanPhoto = { reportId ->
                    navController.navigate("clean_camera/$reportId")
                },
                onBackToHome = { navController.popBackStack() },
                onOpenIncidentDetail = { reportId ->
                    navController.navigate("incident/$reportId")
                },
            )
        }

        composable(
            route = "incident/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.LongType }),
        ) { entry ->
            val reportId = entry.arguments?.getLong("reportId") ?: return@composable
            ReportDetailScreen(
                reportId = reportId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onRequestCleanPhoto = { id ->
                    navController.navigate("clean_camera/$id")
                },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard") {
                        launchSingleTop = true
                    }
                },
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard") {
                        launchSingleTop = true
                    }
                },
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable("leaderboard") {
            LeaderboardScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
                onOpenUserCleanupsMap = { userId, nickname ->
                    val uid = userId ?: -1
                    navController.navigate("user_cleanups_map/$uid/${Uri.encode(nickname)}")
                },
            )
        }

        composable(
            route = "user_cleanups_map/{userId}/{nickname}",
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType },
                navArgument("nickname") { type = NavType.StringType },
            ),
        ) { entry ->
            val userId = entry.arguments?.getInt("userId") ?: -1
            val encoded = entry.arguments?.getString("nickname").orEmpty()
            val nickname = Uri.decode(encoded)
            if (nickname.isBlank()) {
                navController.popBackStack()
                return@composable
            }
            MapScreen(
                viewModel = viewModel,
                cleanerUserIdFilter = userId,
                cleanerNicknameFilter = nickname,
                onReportIncident = {},
                onOpenLeaderboard = {
                    navController.navigate("leaderboard") {
                        launchSingleTop = true
                    }
                },
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
                onRequestCleanPhoto = {},
                onBackToHome = { navController.popBackStack() },
                onOpenIncidentDetail = { reportId ->
                    navController.navigate("incident/$reportId")
                },
            )
        }

        composable("cleanup_success") {
            CleanupSuccessScreen(
                pointsEarned = EcoKarma.MARK_CLEANED,
                onDone = {
                    navController.popBackStack("map", inclusive = false)
                },
                onViewLeaderboard = {
                    navController.navigate("leaderboard") {
                        popUpTo("cleanup_success") { inclusive = true }
                    }
                },
                viewModel = viewModel,
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
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
                onOpenLeaderboard = {
                    navController.navigate("leaderboard") {
                        launchSingleTop = true
                    }
                },
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable("camera") {
            CameraScreen(
                viewModel = viewModel,
                autoOpenCamera = true,
                onBack = { navController.popBackStack() },
                onImageCaptured = { uri ->
                    viewModel.updateCapturedImageUri(uri.toString())
                    navController.navigate("report") {
                        popUpTo("camera") { inclusive = true }
                    }
                },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard") {
                        launchSingleTop = true
                    }
                },
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
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
                viewModel = viewModel,
                title = "Capture cleaning proof",
                sessionKey = "clean_$reportId",
                autoOpenCamera = true,
                onBack = { navController.popBackStack() },
                onImageCaptured = { uri ->
                    viewModel.markReportCleaned(reportId, uri.toString()) { pointsEarned ->
                        if (pointsEarned != null) {
                            navController.navigate("cleanup_success") {
                                popUpTo("clean_camera/$reportId") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                },
                onOpenLeaderboard = {
                    navController.navigate("leaderboard") {
                        launchSingleTop = true
                    }
                },
                onOpenProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
