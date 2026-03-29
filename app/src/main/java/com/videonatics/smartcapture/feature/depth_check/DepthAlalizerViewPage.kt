package com.videonatics.smartcapture.feature.depth_check

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.videonatics.smartcapture.core.centerROI
import com.videonatics.smartcapture.core.computeRange
import com.videonatics.smartcapture.core.extractDepth
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DepthAnalyzerView(
    session: Session
) {
    var resultText by remember { mutableStateOf("Initializing...") }

    // Buffer for smoothing
    val rangeBuffer = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {

        // Configure depth
        val config = session.config
        config.depthMode = Config.DepthMode.AUTOMATIC
        session.configure(config)

        while (true) {
            try {
                val frame = session.update()

                if (frame.camera.trackingState != TrackingState.TRACKING) {
                    resultText = "Tracking..."
                    delay(50)
                    continue
                }

                val depthImage = try {
                    frame.acquireDepthImage16Bits()
                } catch (e: Exception) {
                    null
                }

                if (depthImage != null) {
                    val width = depthImage.width
                    val height = depthImage.height

                    val depthArray = extractDepth(depthImage)

                    val roiValues = centerROI(depthArray, width, height)

                    val range = computeRange(roiValues)

                    // Smooth over last 5 frames
                    rangeBuffer.add(range)
                    if (rangeBuffer.size > 5) rangeBuffer.removeAt(0)

                    val avgRange = rangeBuffer.average()

                    resultText = if (avgRange < 60) {
                        "FLAT (Photo/Screen)"
                    } else {
                        "3D Object"
                    }

                    depthImage.close()
                }

            } catch (e: Exception) {
                resultText = "Error: ${e.message}"
            }

            delay(50) // ~20 FPS processing
        }
    }

    // UI Overlay
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = resultText,
            style = TextStyle(fontSize = 20.sp),
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(12.dp),
            color = Color.White
        )
    }
}