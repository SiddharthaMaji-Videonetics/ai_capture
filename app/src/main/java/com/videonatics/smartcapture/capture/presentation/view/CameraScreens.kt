package com.videonatics.smartcapture.capture.presentation.view

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.videonatics.smartcapture.core.BackendWebSocket
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.ui.BoundaryOverlay
import com.videonatics.smartcapture.ui.CameraHelperGuild
import com.videonatics.smartcapture.ui.DetectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridCameraScreen(padding: PaddingValues,
                       isDepthDetectionEnabled: Boolean,
                       captureTrigger: Boolean,
                       onImageCaptured: (ByteArray) -> Unit,
                       onFullImageCapture: (Bitmap) -> Unit,
                       onArCoreNotInstalled: () -> Unit,
                       isFlashOn: Boolean = false,
                       onCapturableImage : (Boolean)-> Unit,
                       ) {
    var status by remember { mutableStateOf(DetectionStatus.SEARCHING) }
    var detectedBox by remember { mutableStateOf<Rect?>(null) }
    var localStatus by remember { mutableStateOf("Local: Detecting") }
    var backendStatus by remember { mutableStateOf("Backend: Waiting") }
    var cameraHelperGuild by remember { mutableStateOf( CameraHelperGuild.Offset(0f,0f)) }

    val viewmodel = hiltViewModel<SharedCaptureViewModel>()
    val webSocket = remember {

        BackendWebSocket {
            backendStatus = "Backend: $it"
        }.apply { connect() }
    }

    Box (modifier = Modifier.padding(padding)){

        CameraPreviewHybrid(
            captureTrigger = captureTrigger,
            isDepthDetectionEnabled = isDepthDetectionEnabled,
            onLocalResult = {
                localStatus = "Local: ${it.message}"
                Log.e("detection", it.message)

                if(it.message.contains("Perfect")){
                    cameraHelperGuild = CameraHelperGuild.Offset(0f,0f,0f)
                    onCapturableImage.invoke(true)
                }else{
                    cameraHelperGuild = CameraHelperGuild.Offset(it.moveHorizontal.toFloat(),it.moveVertical.toFloat(), it.zoom.toFloat())
//                    val dObjPair = it.split(":").last()
//                    val dPairObj = dObjPair.split(",").first() to dObjPair.split(",").last()
//                    if(dPairObj.first.contains("OK") and dPairObj.second.contains("OK")){
//                        cameraHelperGuild = CameraHelperGuild.Offset(0f,0f)
//                    }else if(dPairObj.first.contains("Left") and dPairObj.second.contains("OK")){
//                        cameraHelperGuild = CameraHelperGuild.Offset(-1f,0f)
//                    }else if(dPairObj.first.contains("Right") and dPairObj.second.contains("OK")){
//                        cameraHelperGuild = CameraHelperGuild.Offset(1f,0f)
//                    }else if(dPairObj.first.contains("OK") and dPairObj.second.contains("Up")){
//                        cameraHelperGuild = CameraHelperGuild.Offset(0f,-1f)
//                    }else if(dPairObj.first.contains("OK") and dPairObj.second.contains("Down")){
//                        cameraHelperGuild = CameraHelperGuild.Offset(0f,1f)
//                    }

                    onCapturableImage.invoke(false)
                }
                    status = if(it.message.contains("Perfect")){
                        DetectionStatus.PERFECT
                    }else if(it.message.contains("Searching")){
                        DetectionStatus.SEARCHING
                    }else if(it.message.contains("outside boundary")){
                        DetectionStatus.OUTSIDE
                    }else if(it.message.contains("Move")){
                        DetectionStatus.PARTIAL
                    }else{
                        DetectionStatus.INSIDE
                    }

                //Log.e("Detection", "Local: $it")
            },

            onFrameCaptured = { bytes ->
                //webSocket.sendFrame(bytes)
                onImageCaptured.invoke(bytes)
                Log.e("Frame", "Captured")
            },
            onFullImageCaptured = {
                onFullImageCapture.invoke(it)
            },
            onBoundingBox = {
                Log.d("isBox", viewmodel.isBoundBoxEnabled().toString())
                if(viewmodel.isBoundBoxEnabled()){
                detectedBox = it
                }
            },
            isBoundingBoxEnabled = viewmodel.isBoundBoxEnabled(),
            onArCoreNotInstalled = onArCoreNotInstalled,
            torchEnabled = isFlashOn
        )

        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {

            Text(localStatus, color = Color.White)

            //Text(backendStatus, color = Color.Green)
        }

        BoundaryOverlay(
            detectedBox = detectedBox,
            status = status,
            cameraHelperGuild = cameraHelperGuild
        )

    }
}