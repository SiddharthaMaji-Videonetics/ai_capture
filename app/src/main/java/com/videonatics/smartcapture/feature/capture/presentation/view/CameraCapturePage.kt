package com.videonatics.smartcapture.feature.capture.presentation.view

import android.Manifest
import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.HapticManager
import com.videonatics.smartcapture.core.LightSensorManager
import com.videonatics.smartcapture.core.TiltPopup
import com.videonatics.smartcapture.core.isLowLight
import com.videonatics.smartcapture.core.presentation.view.components.rememberHapticManager
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.core.rememberTiltState
import com.videonatics.smartcapture.core.showToast
import com.videonatics.smartcapture.feature.capture.domain.model.Tilt
import com.videonatics.smartcapture.feature.capture.presentation.view.components.DepthScanningBottomSheet
import com.videonatics.smartcapture.feature.capture.presentation.view.components.LocationPermission
import com.videonatics.smartcapture.feature.capture.presentation.view.components.rememberYaw
import com.videonatics.smartcapture.feature.capture.presentation.viewmodel.CaptureViewModel
import com.videonatics.smartcapture.feature.home.presentation.view.HomePageComponent
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.LocalActivatedComponentPrimaryColor
import com.videonatics.smartcapture.ui.theme.LocalTextPrimaryColor
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraCapturePage(navController: NavController,
                      sharedViewModel: SharedCaptureViewModel? = null) {

    val viewModel = hiltViewModel<CaptureViewModel>()
    val location by viewModel.location.collectAsState()

    LocationPermission {
        viewModel.startLocationUpdates()
    }

    if (location != null) {
        Log.e("location","Lat: ${location!!.latitude}, Lon: ${location!!.longitude}")
    }
    return CameraCapturePageComponent(sharedViewModel,onImageCapture = {
        location?.apply {
            sharedViewModel?.capturedLocation = location
        }

        if(sharedViewModel?.isDepthDetectionEnabled()?:false){
            navController.navigate(PageRoute.DEPTH_CHECK.name) {
                popUpTo(PageRoute.CAPTURE.name) {
                    inclusive = true
                }
            }
        }else{
            navController.navigate(PageRoute.OCR_SELECT.name){
                popUpTo(PageRoute.CAPTURE.name){
                    inclusive = true
                }
            }
        }
    })

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCapturePageComponent(sharedViewModel: SharedCaptureViewModel? = null, onImageCapture: () -> Unit = {}) {
    val context = LocalContext.current
    var captureTrigger by remember { mutableStateOf(false) }
    var capturableImage by remember {mutableStateOf(false)}
    var isTooMuchTilt by remember { mutableStateOf(false) }
    var installArCorePrompt by remember { mutableStateOf(false) }
    var isDepthScanning by remember { mutableStateOf(false) }
    val noOfConfirmation = 1
    var object3dConfirmationCount by remember { mutableStateOf(noOfConfirmation) } //need to confirm 5 times side be side

    val yaw by rememberYaw()

    val depthScanBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    //---
    val scope = rememberCoroutineScope()

    val tilt by rememberTiltState()

    var isTorchEnabled by remember { mutableStateOf(false) }
    val lightSensorManager = remember { LightSensorManager(context) }
    val lux by lightSensorManager.lux.collectAsState()

    LaunchedEffect(Unit) {
        lightSensorManager.start()
        isDepthScanning = false
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val haptic = rememberHapticManager()

    return Scaffold (
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
                haptic.perform(HapticManager.Type.SUCCESS)
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

    ){ padding ->
        Box(modifier = Modifier.fillMaxSize()){

            HybridCameraScreen(padding,
                isDepthDetectionEnabled = sharedViewModel?.isDepthDetectionEnabled()?:false,
                isDepthScanning = isDepthScanning,
                captureTrigger,
                onImageCaptured = { byteArray ->
                    captureTrigger = false
                    sharedViewModel?.capturedImage = byteArray
                    scope.launch {
                        onImageCapture.invoke()
                    }


                },
                onArCoreNotInstalled = {
                    installArCorePrompt = true
                },
                onFullImageCapture = {
                    sharedViewModel?.capturedFullImage = it
                },
                onCapturedImageStream = {
                    if(isDepthScanning){
                        sharedViewModel?.apply {
                            sharedViewModel.cropAndProcessImage(it){crpImg ->
                                sharedViewModel.capturedImageSeq.add(crpImg)
                            }

                            if (sharedViewModel.capturedImageSeq.size >= sharedViewModel.depthImageNoThreshold) {
                                // Run analysis
                                sharedViewModel.analyzeDepth(sharedViewModel.capturedImageSeq) { isReal ->
                                    if (isReal) {
                                        Log.d("ParallaxCheck", "Real 3D object detected")
                                        if(object3dConfirmationCount >0){
                                            object3dConfirmationCount--
                                            //sharedViewModel.capturedImageSeq.clear()
                                        }else{
                                            //sharedViewModel.capturedImageSeq.clear()
                                            //context.showToast("Real 3D object detected")

                                            scope.launch {
                                                depthScanBottomSheetState.hide()
                                                delay(500)
                                                isDepthScanning = false
                                                object3dConfirmationCount =0
                                            }
                                        }


                                    }
                                    else {
                                        Log.d("ParallaxCheck", "Flat photo detected")
                                        if(object3dConfirmationCount <=noOfConfirmation){
                                            object3dConfirmationCount++
                                            context.showToast("move your phone slowly left and right.")
                                        }

                                    }
                                    sharedViewModel.trimDepthImageSeq()
                                }
                            }
                        }
                    }


                },
                isFlashOn = isTorchEnabled,
                isBoundingBoxEnabled = sharedViewModel?.isBoundBoxEnabled() ?: false) {
                capturableImage = it
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(5.dp)
                    .background(
                        Color.White.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )

                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vtpl_logo),
                    contentDescription = "Brand Logo",
                    modifier = Modifier.size(width = 52.dp, height = 30.dp)
                )
            }
            AnimatedVisibility(visible = isTorchEnabled,
                enter = scaleIn(
                    initialScale = 0.6f
                ) + fadeIn(),

                exit = scaleOut(
                    targetScale = 0.6f
                ) + fadeOut(
                ),
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
                ) {
                IconButton(onClick = {
                    // navController.navigate(PageRoute.SETTINGS.name)
                }) {
                    Icon(
                        imageVector = Icons.Default.FlashAuto,
                        contentDescription = "Flsh",
                        tint = Color(0xFFFFC107)
                    )
                }
            }
            //tilt popup
            if (kotlin.math.abs(tilt.pitch) > 10 || kotlin.math.abs(tilt.pitch) < -10) {
                isTooMuchTilt = true
                TiltPopup()
            }else{
                isTooMuchTilt = false
            }


//            AnimatedVisibility (visible = installArCorePrompt){
//                //InstallArCorePopup()
//            }

            //TiltComponent(tilt)

            if(isDepthScanning){
                DepthScanningBottomSheet(depthScanBottomSheetState,
                    yaw) {
                    isDepthScanning = true
                }
            }
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

@Preview(showBackground = true)
@Composable
fun CapturePagePreview() {
    SmartCaptureTheme {
        CameraCapturePageComponent {

        }
    }
}