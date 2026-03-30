package com.videonatics.smartcapture.core.presentation.view.components

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.videonatics.smartcapture.core.domain.model.OcrBox

@Composable
fun ZoomableImage(bitmap: ImageBitmap) {

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->

                    // Apply zoom
                    scale = (scale * zoom).coerceIn(1f, 5f)

                    // Apply pan
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {

        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
fun ZoomableImageWithSelectableOCR(
    bitmap: ImageBitmap,
    ocrBoxes: List<OcrBox>
) {
    val minScale = 1f
    val maxScale = 5f

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedId by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
    ) {

        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        val baseScale = containerWidth / imageWidth

        val state = rememberTransformableState { zoomChange, panChange, _ ->

            val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)

            val scaledImageWidth = imageWidth * baseScale * newScale
            val scaledImageHeight = imageHeight * baseScale * newScale

            val maxX = maxOf(0f, (scaledImageWidth - containerWidth) / 2f)
            val maxY = maxOf(0f, (scaledImageHeight - containerHeight) / 2f)

            val newOffset = offset + (panChange * newScale)

            offset = Offset(
                x = newOffset.x.coerceIn(-maxX, maxX),
                y = newOffset.y.coerceIn(-maxY, maxY)
            )

            scale = newScale
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state)
                .pointerInput(scale, offset) {
                    detectTapGestures { tap ->

                        // Convert screen → image coordinates

                        val x1 = tap.x - offset.x
                        val y1 = tap.y - offset.y

                        val x2 = x1 / scale
                        val y2 = y1 / scale

                        val imageX = x2 / baseScale
                        val imageY = y2 / baseScale

                        val finalX = imageX
                        val finalY = imageY

                        selectedId = ocrBoxes.find {
                            it.rect.contains(finalX, finalY)
                        }?.id
                    }
                }
        ) {

            // TRANSFORMATION LAYER
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {

                // Image
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize()
                )

                // OCR Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {

                    ocrBoxes.forEach { box ->

                        val isSelected = box.id == selectedId

                        val left = box.rect.left * baseScale
                        val top = box.rect.top * baseScale
                        val right = box.rect.right * baseScale
                        val bottom = box.rect.bottom * baseScale

                        val width = right - left
                        val height = bottom - top

                        // Selected background
                        if (isSelected) {
                            drawRect(
                                color = Color.Yellow.copy(alpha = 0.4f),
                                topLeft = Offset(left, top),
                                size = Size(width, height)
                            )
                        }

                        // Border
                        drawRect(
                            color = if (isSelected) Color.Yellow else Color.Red,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = if (isSelected) 5f else 3f)
                        )

                        // Text background
                        val textBgHeight = 36f

                        drawRect(
                            color = if (isSelected)
                                Color(0xFF333300)
                            else
                                Color(0xAA000000),
                            topLeft = Offset(left, top - textBgHeight),
                            size = Size(width, textBgHeight)
                        )

                        // Text
                        drawContext.canvas.nativeCanvas.drawText(
                            box.text,
                            left + 6f,
                            top - 8f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 28f
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PixelPerfectOCRViewer(
    bitmap: ImageBitmap,
    ocrBoxes: List<OcrBox>,
    onSelectedOcrIds: (List<Int>) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()

        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        val fitScale = minOf(containerW / imgW, containerH / imgH)

        val displayW = imgW * fitScale
        val displayH = imgH * fitScale

        val imageOffsetX = (containerW - displayW) / 2f
        val imageOffsetY = (containerH - displayH) / 2f

        val transformState = rememberTransformableState { zoom, pan, _ ->
            val newScale = (scale * zoom).coerceIn(1f, 5f)
            val isZooming = zoom != 1f
            scale = newScale
            if (!isZooming) offset += pan
        }

        fun mapToImageSpace(tap: Offset): Offset {
            val translatedX = tap.x - offset.x
            val translatedY = tap.y - offset.y

            val scaledX = translatedX / scale
            val scaledY = translatedY / scale

            val x = (scaledX - imageOffsetX) / displayW
            val y = (scaledY - imageOffsetY) / displayH

            return Offset(x, y)
        }

        fun isInside(x: Float, y: Float, points: List<PointF>): Boolean {
            var inside = false
            var j = points.lastIndex

            for (i in points.indices) {
                val xi = points[i].x
                val yi = points[i].y
                val xj = points[j].x
                val yj = points[j].y

                val intersect = ((yi > y) != (yj > y)) &&
                        (x < (xj - xi) * (y - yi) / (yj - yi + 0.00001f) + xi)

                if (intersect) inside = !inside
                j = i
            }
            return inside
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
                .pointerInput(scale, offset) {
                    detectTapGestures { tap ->
                        val p = mapToImageSpace(tap)

                        val tappedId = ocrBoxes.find { box ->
                            isInside(p.x, p.y, box.cornerPoints)
                        }?.id

                        tappedId?.let { id ->
                            selectedIds = if (selectedIds.contains(id)) {
                                selectedIds - id
                            } else {
                                selectedIds + id
                            }
                        }

                        onSelectedOcrIds(selectedIds.toList())
                    }
                }
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun mapX(x: Float) = x * displayW + imageOffsetX
                    fun mapY(y: Float) = y * displayH + imageOffsetY

                    ocrBoxes.forEach { box ->
                        if (box.cornerPoints.size != 4) return@forEach

                        val path = Path().apply {
                            val p0 = box.cornerPoints[0]
                            val p1 = box.cornerPoints[1]
                            val p2 = box.cornerPoints[2]
                            val p3 = box.cornerPoints[3]

                            moveTo(mapX(p0.x), mapY(p0.y))
                            lineTo(mapX(p1.x), mapY(p1.y))
                            lineTo(mapX(p2.x), mapY(p2.y))
                            lineTo(mapX(p3.x), mapY(p3.y))
                            close()
                        }

                        val isSelected = selectedIds.contains(box.id)

                        if (isSelected) {
                            drawPath(
                                path = path,
                                color = Color(0xFF008606).copy(alpha = 0.3f)
                            )
                        }

                        drawPath(
                            path = path,
                            color = if (isSelected) Color(0xFF008606) else Color.Red,
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }
    }
}