package com.videonatics.smartcapture.feature.depth_check

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.ui.BoundaryPercent
import com.videonatics.smartcapture.ui.DetectionStatus

@Composable
fun ARBoundaryOverlay(
    boundaryPercent: BoundaryPercent = BoundaryPercent(),
) {
    val overlayColor = Color.Black.copy(alpha = 0.6f)
    val borderColor = Color(0xFF00E5FF)

    // 🔳 Smooth continuous animation
    val infiniteTransition = rememberInfiniteTransition(label = "grid_anim")

    val gridSpacingPx = with(LocalDensity.current) { 40.dp.toPx() }

    val rawOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = gridSpacingPx,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_shift"
    )

    // seamless tiling
    val offset = rawOffset % gridSpacingPx

    Canvas(modifier = Modifier.fillMaxSize()) {

        val left = size.width * boundaryPercent.left
        val top = size.height * boundaryPercent.top
        val right = size.width * boundaryPercent.right
        val bottom = size.height * boundaryPercent.bottom

        val rectWidth = right - left
        val rectHeight = bottom - top

        val cornerRadius = 24f

        val borderStroke = 4.dp.toPx()
        val cornerStroke = 6.dp.toPx()
        val gap = 6.dp.toPx()
        val offsetCorner = borderStroke / 2 + gap
        val cornerLength = 32.dp.toPx()

        val cutoutRect = Rect(left, top, right, bottom)

        // 🔲 Dark overlay with rounded cutout
        drawPath(
            path = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addRoundRect(
                    RoundRect(
                        rect = cutoutRect,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
                fillType = PathFillType.EvenOdd
            },
            color = overlayColor
        )

        // ✨ Border
        drawRoundRect(
            color = borderColor.copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = borderStroke)
        )

        // 🔳 Smooth moving grid (tiled, no jump)
        val gridAlpha = 0.15f

        clipRect(left, top, right, bottom) {

            // Vertical lines
            var x = left - gridSpacingPx + offset
            while (x <= right) {
                drawLine(
                    color = borderColor.copy(alpha = gridAlpha),
                    start = Offset(x, top),
                    end = Offset(x, bottom),
                    strokeWidth = 1.dp.toPx()
                )
                x += gridSpacingPx
            }

            // Horizontal lines
            var y = top - gridSpacingPx + offset
            while (y <= bottom) {
                drawLine(
                    color = borderColor.copy(alpha = gridAlpha),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += gridSpacingPx
            }
        }

        // 🔲 Corner accents (outside, no overlap)
        fun drawCorner(x: Float, y: Float, isLeft: Boolean, isTop: Boolean) {

            val startX = if (isLeft) x - offsetCorner else x + offsetCorner
            val startY = if (isTop) y - offsetCorner else y + offsetCorner

            // Horizontal
            drawLine(
                color = borderColor,
                start = Offset(startX, startY),
                end = Offset(
                    startX + if (isLeft) cornerLength else -cornerLength,
                    startY
                ),
                strokeWidth = cornerStroke,
                cap = StrokeCap.Round
            )

            // Vertical
            drawLine(
                color = borderColor,
                start = Offset(startX, startY),
                end = Offset(
                    startX,
                    startY + if (isTop) cornerLength else -cornerLength
                ),
                strokeWidth = cornerStroke,
                cap = StrokeCap.Round
            )
        }

        drawCorner(left, top, true, true)
        drawCorner(right, top, false, true)
        drawCorner(left, bottom, true, false)
        drawCorner(right, bottom, false, false)

        // 🌈 Subtle inner gradient (depth feel)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    borderColor.copy(alpha = 0.08f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
    }
}