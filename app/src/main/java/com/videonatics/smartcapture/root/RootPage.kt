package com.videonatics.smartcapture.root

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.capture.presentation.view.CameraCapturePage
import com.videonatics.smartcapture.feature.depth_check.ARFlatnessScreen
import com.videonatics.smartcapture.feature.home.presentation.view.HomePage
import com.videonatics.smartcapture.feature.imagedetails.presentation.view.ImageDetailsPage
import com.videonatics.smartcapture.feature.settings.presentation.view.SettingsPage
import com.videonatics.smartcapture.feature.splash.SplashScreen
import com.videonatics.smartcapture.ui.theme.ThemeMode


@Composable
fun RootPage(onThemeSwitch: (ThemeMode) -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController,
        startDestination = "main_graph"
    ) {
        navigation(
            startDestination = PageRoute.SPLASH.name,
            route = "main_graph"
        ){
            composable(PageRoute.SPLASH.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel =
                    hiltViewModel(parentEntry)
                onThemeSwitch.invoke(sharedViewModel.themeMode)
                SplashScreen(navController)
            }
            composable(PageRoute.HOME.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCaptureViewModel =
                    hiltViewModel(parentEntry)
                HomePage(navController)
            }

            composable(PageRoute.CAPTURE.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCaptureViewModel =
                    hiltViewModel(parentEntry)
                CameraCapturePage(navController, sharedViewModel)
            }
            composable(PageRoute.DEPTH_CHECK.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCaptureViewModel =
                    hiltViewModel(parentEntry)
                ARFlatnessScreen(navController,onResult = {
                    Log.e("isFlat", it.toString())
                }){

                }
            }
            composable(PageRoute.IMAGE_DETAILS.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCaptureViewModel =
                    hiltViewModel(parentEntry)
                ImageDetailsPage(navController, sharedViewModel)
            }

            composable(PageRoute.SETTINGS.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCaptureViewModel =
                    hiltViewModel(parentEntry)
                SettingsPage(navController, sharedViewModel){
                    onThemeSwitch.invoke(it)
                }
            }
        }


    }

}

enum class PageRoute{
    SPLASH,HOME, CAPTURE,IMAGE_DETAILS, SETTINGS, DEPTH_CHECK
}