package com.videonatics.smartcapture.capture.presentation.view

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.videonatics.smartcapture.capture.domain.model.Tilt
import com.videonatics.smartcapture.core.LightSensorManager
import com.videonatics.smartcapture.core.TiltPopup
import com.videonatics.smartcapture.core.isLowLight
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.core.rememberTiltState
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.LocalActivatedComponentPrimaryColor
import com.videonatics.smartcapture.ui.theme.LocalTextPrimaryColor
import kotlinx.coroutines.launch
import java.util.Stack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCapturePage(navController: NavController,
                      sharedViewModel: SharedCaptureViewModel) {
    val context = LocalContext.current
    var captureTrigger by remember { mutableStateOf(false) }
    var capturableImage by remember {mutableStateOf(false)}
    var isTooMuchTilt by remember { mutableStateOf(false) }
    var installArCorePrompt by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val tilt by rememberTiltState()

    var isTorchEnabled by remember { mutableStateOf(false) }
    val lightSensorManager = remember { LightSensorManager(context) }
    val lux by lightSensorManager.lux.collectAsState()

    LaunchedEffect(Unit) {
        lightSensorManager.start()
    }

    LaunchedEffect(lux) {
        val shouldEnableTorch = isLowLight(lux)
        if (shouldEnableTorch != isTorchEnabled) {
            isTorchEnabled = shouldEnableTorch
            Log.e("light", if (shouldEnableTorch) "on" else "off")
        }
    }

    DisposableEffect(Unit) {
        onDispose { lightSensorManager.stop() }
    }

    Scaffold (
        topBar = {
            TopAppBar(
                title = {
                    Text("Smart Capture")
                },
                actions = {
                    AnimatedVisibility(visible = isTorchEnabled) {
                        IconButton(onClick = {
                            // navController.navigate(PageRoute.SETTINGS.name)
                        }) {
                            Icon(
                                imageVector = Icons.Default.FlashAuto,
                                contentDescription = "Flsh",
                                tint = LocalActivatedComponentPrimaryColor.current
                            )
                        }
                    }

                    IconButton(onClick = {
                        navController.navigate(PageRoute.SETTINGS.name)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = LocalTextPrimaryColor.current
                        )
                    }

                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            AnimatedVisibility(
                visible = capturableImage && !isTooMuchTilt,
                enter = scaleIn(
                    initialScale = 0.6f
                ) + fadeIn(),

                exit = scaleOut(
                    targetScale = 0.6f
                ) + fadeOut()
            ) {
                FloatingActionButton(
                    containerColor = Color(0xFF3F51B5),
                    onClick = {
                            captureTrigger = true
                            capturableImage = false

                    }) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        tint = Color.White
                    )
                }
            }


        },
//        bottomBar = {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//
////                Text("Pitch: ${tilt.pitch.toInt()}°")
////                Text("Roll: ${tilt.roll.toInt()}°")
//
////                if (kotlin.math.abs(tilt.pitch) > 15) {
////                    Text("Too much forward/backward tilt!")
////                }
//
//                if (kotlin.math.abs(tilt.roll) > 15) {
//                    Text("Too much left/right tilt!")
//                }
//            }
//        }
    ){ padding ->
        Box(modifier = Modifier.fillMaxSize()){
            HybridCameraScreen(padding,
                isDepthDetectionEnabled = sharedViewModel.isDepthDetectionEnabled(),
                captureTrigger,
                onImageCaptured = { byteArray ->
                captureTrigger = false
                sharedViewModel.capturedImage = byteArray
                scope.launch {

                    if(sharedViewModel.isDepthDetectionEnabled()){
                        navController.navigate(PageRoute.DEPTH_CHECK.name) {
                            popUpTo(PageRoute.CAPTURE.name) {
                                inclusive = true
                            }
                        }
                    }else{
                        navController.navigate(PageRoute.IMAGE_DETAILS.name){
                        popUpTo(PageRoute.CAPTURE.name){
                            inclusive = true
                        }
                    }
                    }



                }


            },
                onArCoreNotInstalled = {
                    installArCorePrompt = true
                },
                onFullImageCapture = {
                sharedViewModel.capturedFullImage = it
                },
                isFlashOn = isTorchEnabled) {
                capturableImage = it
            }

            //tilt popup
            if (kotlin.math.abs(tilt.pitch) > 5 || kotlin.math.abs(tilt.pitch) < -5) {
                isTooMuchTilt = true
                TiltPopup()
            }else{
                isTooMuchTilt = false
            }


//            AnimatedVisibility (visible = installArCorePrompt){
//                //InstallArCorePopup()
//            }

            //TiltComponent(tilt)
        }

    }
}

@Composable
fun TiltComponent(tilt: Tilt) {
    return Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ){
        Column() {
            Text("Pitch: ${tilt.pitch.toInt()}°")
            Text("Roll: ${tilt.roll.toInt()}°")
        }

    }
}