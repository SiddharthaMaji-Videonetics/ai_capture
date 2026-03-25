package com.videonatics.smartcapture.ui

import android.graphics.Rect
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.R

data class BoundaryPercent(
    val left: Float = 0.2f,
    val top: Float = 0.3f,
    val right: Float = 0.8f,
    val bottom: Float = 0.7f
)

enum class DetectionStatus {
    SEARCHING,
    OUTSIDE,
    PARTIAL,
    INSIDE,
    PERFECT
}

sealed class CameraHelperGuild(){
    data class Offset(val x: Float, val y: Float, val z: Float = 0f) : CameraHelperGuild()
}


@Composable
fun BoundaryOverlay(
    boundaryPercent: BoundaryPercent = BoundaryPercent(),
    detectedBox: Rect? = null,
    status: DetectionStatus = DetectionStatus.SEARCHING,
    cameraHelperGuild: CameraHelperGuild = CameraHelperGuild.Offset(0f,0f)
) {
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val iconSize = 76.dp
    val margin = 12.dp

    val iconSizePx = with(density) { iconSize.toPx() }
    val marginPx = with(density) { margin.toPx() }

    fun pxToDp(px: Float) = with(density) { px.toDp() }


    val boundaryColor =
        when (status) {
            DetectionStatus.SEARCHING -> Color.Gray
            DetectionStatus.OUTSIDE -> Color.Red
            DetectionStatus.PARTIAL -> Color.Yellow
            DetectionStatus.INSIDE -> Color(0xFF4CAF50)
            DetectionStatus.PERFECT -> Color.Green
        }

    val objectColor =
        when (status) {
            DetectionStatus.SEARCHING -> Color.Transparent
            DetectionStatus.OUTSIDE -> Color.Red
            DetectionStatus.PARTIAL -> Color.Yellow
            DetectionStatus.INSIDE -> Color(0xFF4CAF50)
            DetectionStatus.PERFECT -> Color.Green
        }

    val infiniteTransition = rememberInfiniteTransition(label = "blink")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnim"
    )


    Canvas(modifier = Modifier.fillMaxSize()) {

        val boundaryLeft = size.width * boundaryPercent.left
        val boundaryTop = size.height * boundaryPercent.top
        val boundaryRight = size.width * boundaryPercent.right
        val boundaryBottom = size.height * boundaryPercent.bottom

        val overlayColor = Color.Black.copy(alpha = 0.5f)

        // TOP overlay
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, boundaryTop)
        )

        // BOTTOM overlay
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, boundaryBottom),
            size = Size(size.width, size.height - boundaryBottom)
        )

        // LEFT overlay
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, boundaryTop),
            size = Size(boundaryLeft, boundaryBottom - boundaryTop)
        )

        // RIGHT overlay
        drawRect(
            color = overlayColor,
            topLeft = Offset(boundaryRight, boundaryTop),
            size = Size(size.width - boundaryRight, boundaryBottom - boundaryTop)
        )

        // Boundary rectangle
        drawRect(
            color = boundaryColor,
            topLeft = Offset(boundaryLeft, boundaryTop),
            size = Size(
                boundaryRight - boundaryLeft,
                boundaryBottom - boundaryTop
            ),
            style = Stroke(width = 6f)
        )

        // Detected object box
        detectedBox?.let {
            drawRect(
                color = objectColor,
                topLeft = Offset(it.left.toFloat(), it.top.toFloat()),
                size = Size(it.width().toFloat(), it.height().toFloat()),
                style = Stroke(width = 4f)
            )
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { parentSize = it }
    ) {

        var leftArrowVisibility by remember { mutableStateOf(false) }
        var topArrowVisibility by remember { mutableStateOf(false) }
        var rightArrowVisibility by remember { mutableStateOf(false) }
        var bottomArrowVisibility by remember { mutableStateOf(false) }
        var zoomInVisibility by remember { mutableStateOf(false) }
        var zoomOutVisibility by remember { mutableStateOf(false) }



        Canvas(modifier = Modifier.matchParentSize()) {

            val boundaryLeft = size.width * boundaryPercent.left
            val boundaryTop = size.height * boundaryPercent.top
            val boundaryRight = size.width * boundaryPercent.right
            val boundaryBottom = size.height * boundaryPercent.bottom
            Log.e("CameraHelperGuild", "Offset: $cameraHelperGuild")
            when(cameraHelperGuild){

                is CameraHelperGuild.Offset -> {
                    if(cameraHelperGuild.x !=0f){
                        leftArrowVisibility = cameraHelperGuild.x > 0
                        rightArrowVisibility = cameraHelperGuild.x < 0
                    }else{
                        leftArrowVisibility = false
                        rightArrowVisibility = false

                    }

                    if(cameraHelperGuild.y !=0f){
                        topArrowVisibility = cameraHelperGuild.y > 0
                        bottomArrowVisibility = cameraHelperGuild.y < 0

                    }else{
                        topArrowVisibility = false
                        bottomArrowVisibility = false
                    }

                    if(cameraHelperGuild.z !=0f){
                        zoomInVisibility = cameraHelperGuild.z > 0
                        zoomOutVisibility = cameraHelperGuild.z < 0

                    }else{
                        zoomInVisibility = false
                        zoomOutVisibility = false
                    }


                }
            }
            drawRect(
                color = boundaryColor,
                topLeft = Offset(boundaryLeft, boundaryTop),
                size = Size(
                    boundaryRight - boundaryLeft,
                    boundaryBottom - boundaryTop
                ),
                style = Stroke(width = 6f)
            )

            detectedBox?.let {
                drawRect(
                    color = objectColor,
                    topLeft = Offset(it.left.toFloat(), it.top.toFloat()),
                    size = Size(it.width().toFloat(), it.height().toFloat()),
                    style = Stroke(width = 4f)
                )
            }
        }



        if (parentSize != IntSize.Zero) {

            val boundaryLeftPx = parentSize.width * boundaryPercent.left
            val boundaryTopPx = parentSize.height * boundaryPercent.top
            val boundaryRightPx = parentSize.width * boundaryPercent.right
            val boundaryBottomPx = parentSize.height * boundaryPercent.bottom

            val boundaryCenterXPx = (boundaryLeftPx + boundaryRightPx) / 2
            val boundaryCenterYPx = (boundaryTopPx + boundaryBottomPx) / 2


            //  TOP
            AnimatedVisibility(visible = topArrowVisibility) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropUp,
                    contentDescription = null,
                    tint = boundaryColor.copy(alpha = alpha),
                    modifier = Modifier
                        .offset(
                            x = pxToDp(boundaryCenterXPx - iconSizePx / 2),
                            y = pxToDp(boundaryTopPx - iconSizePx - marginPx)
                        )
                        .size(iconSize)
                )
            }


//  BOTTOM
            AnimatedVisibility(visible = bottomArrowVisibility) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = boundaryColor.copy(alpha = alpha),
                    modifier = Modifier
                        .offset(
                            x = pxToDp(boundaryCenterXPx - iconSizePx / 2),
                            y = pxToDp(boundaryBottomPx + marginPx)
                        )
                        .size(iconSize)
                )
            }

//  LEFT
            AnimatedVisibility(visible = leftArrowVisibility) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowLeft,
                    contentDescription = null,
                    tint = boundaryColor.copy(alpha = alpha),
                    modifier = Modifier
                        .offset(
                            x = pxToDp(boundaryLeftPx - iconSizePx - marginPx),
                            y = pxToDp(boundaryCenterYPx - iconSizePx / 2)
                        )
                        .size(iconSize)
                )
            }

//  RIGHT
            AnimatedVisibility(visible = rightArrowVisibility) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowRight,
                    contentDescription = null,
                    tint = boundaryColor.copy(alpha = alpha),
                    modifier = Modifier
                        .offset(
                            x = pxToDp(boundaryRightPx + marginPx),
                            y = pxToDp(boundaryCenterYPx - iconSizePx / 2)
                        )
                        .size(iconSize)
                )
            }

            //for Zoom

            // ZOOM IN
            AnimatedVisibility(visible = zoomInVisibility) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = boundaryColor.copy(alpha = alpha),
                    modifier = Modifier
                        .offset(
                            x = pxToDp(boundaryCenterXPx - iconSizePx / 2),
                            y = pxToDp(boundaryCenterYPx - iconSizePx / 2),
                        )
                        .size(iconSize)
                )
            }

// ZOOM OUT
            AnimatedVisibility(visible = zoomOutVisibility) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = null,
                    tint = boundaryColor.copy(alpha = alpha),
                    modifier = Modifier
                        .offset(
                            x = pxToDp(boundaryCenterXPx - iconSizePx / 2),
                            y = pxToDp(boundaryCenterYPx - iconSizePx / 2),
                        )
                        .size(iconSize)
                )
            }
        }
    }
}