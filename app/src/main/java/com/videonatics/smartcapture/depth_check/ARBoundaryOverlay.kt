package com.videonatics.smartcapture.depth_check

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import com.videonatics.smartcapture.ui.BoundaryPercent
import com.videonatics.smartcapture.ui.DetectionStatus

@Composable
fun ARBoundaryOverlay(
    boundaryPercent: BoundaryPercent = BoundaryPercent(),
    status: DetectionStatus = DetectionStatus.SEARCHING,
    detectedBox: Rect? = null
) {

    val boundaryColor = when (status) {
        DetectionStatus.SEARCHING -> Color.Gray
        DetectionStatus.OUTSIDE -> Color.Red
        DetectionStatus.PARTIAL -> Color.Yellow
        DetectionStatus.INSIDE -> Color(0xFF4CAF50)
        DetectionStatus.PERFECT -> Color.Green
    }

    val overlayColor = Color.Black.copy(alpha = 0.5f)

    Canvas(modifier = Modifier.fillMaxSize()) {

        val left = size.width * boundaryPercent.left
        val top = size.height * boundaryPercent.top
        val right = size.width * boundaryPercent.right
        val bottom = size.height * boundaryPercent.bottom

        // 🔲 Dark outside area (mask)
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, top)
        )

        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, bottom),
            size = Size(size.width, size.height - bottom)
        )

        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, top),
            size = Size(left, bottom - top)
        )

        drawRect(
            color = overlayColor,
            topLeft = Offset(right, top),
            size = Size(size.width - right, bottom - top)
        )

        // 🟩 Main boundary rectangle
        drawRect(
            color = boundaryColor,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(width = 5f)
        )

        //  Optional detected box (from ML / AR / CV)
        detectedBox?.let {
            drawRect(
                color = boundaryColor,
                topLeft = Offset(it.left.toFloat(), it.top.toFloat()),
                size = Size(it.width().toFloat(), it.height().toFloat()),
                style = Stroke(width = 3f)
            )
        }
    }
}