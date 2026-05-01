package com.example.paryavaran_kavalu.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.paryavaran_kavalu.ui.screens.MapScreen
import com.example.paryavaran_kavalu.ui.screens.ReportScreen
import com.example.paryavaran_kavalu.ui.screens.CameraScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "map"
    ) {

        // 🗺️ MAP SCREEN
        composable("map") {
            MapScreen(
                onAddClick = {
                    navController.navigate("report")
                }
            )
        }

        // 📝 REPORT SCREEN
        composable("report") {
            ReportScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenCamera = {
                    navController.navigate("camera")
                }
            )
        }

        // 📸 CAMERA SCREEN
        composable("camera") {
            CameraScreen(
                onBack = {
                    navController.popBackStack()
                },
                onImageCaptured = {
                    navController.popBackStack() // go back to report
                }
            )
        }
    }
}