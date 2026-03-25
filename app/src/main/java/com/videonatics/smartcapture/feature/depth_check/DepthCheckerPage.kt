package com.videonatics.smartcapture.feature.depth_check

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.ar_core.ARRenderer
import com.videonatics.smartcapture.ui.BoundaryPercent
import com.videonatics.smartcapture.ui.DetectionStatus
import kotlinx.coroutines.launch

@Composable
fun ARFlatnessScreen(
    navController: NavController,
    onResult: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var worldInfo by remember { mutableStateOf("") }

    //  Create GLSurfaceView ONLY once
    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)

            setRenderer(
                ARRenderer(context) { isFlat ->
                    scope.launch {
                        onResult(isFlat)
                        worldInfo = if (isFlat) "Flat" else "Not Flat"
                    }
                }
            )

            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    //  Proper lifecycle handling (ONLY ONCE)
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    Box(Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { glView } //  reuse same view
        )

        ARBoundaryOverlay(
            boundaryPercent = BoundaryPercent(),
            status = DetectionStatus.SEARCHING
        )

        // UI overlay
        Text(
            "world info > $worldInfo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            color = Color.White
        )

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}