package com.videonatics.smartcapture.core.presentation.view.components

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

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        offset += panChange * newScale
        scale = newScale
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
            .transformable(state)
    ) {

        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        val baseScale = containerWidth / imageWidth

        val scaledImageWidth = imageWidth * baseScale * scale
        val scaledImageHeight = imageHeight * baseScale * scale

        val maxX = maxOf(0f, (scaledImageWidth - containerWidth) / 2f)
        val maxY = maxOf(0f, (scaledImageHeight - containerHeight) / 2f)

        offset = Offset(
            x = offset.x.coerceIn(-maxX, maxX),
            y = offset.y.coerceIn(-maxY, maxY)
        )

        //  Gesture detection for taps
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(scale, offset) {
                    detectTapGestures { tap ->

                        // Convert tap -> image coordinate space
                        val adjustedX = (tap.x - offset.x) / scale
                        val adjustedY = (tap.y - offset.y) / scale

                        val mappedX = adjustedX / baseScale
                        val mappedY = adjustedY / baseScale

                        // Find clicked box
                        selectedId = ocrBoxes.find {
                            it.rect.contains(Offset(mappedX, mappedY))
                        }?.id
                    }
                }
        ) {

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

                //  Image
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize()
                )

                //  OCR Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {

                    val scaleX = size.width / imageWidth
                    val scaleY = size.height / imageHeight

                    ocrBoxes.forEach { box ->

                        val left = box.rect.left * scaleX
                        val top = box.rect.top * scaleY
                        val width = box.rect.width * scaleX
                        val height = box.rect.height * scaleY

                        val isSelected = box.id == selectedId

                        //  Highlight background when selected
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
                        drawRect(
                            color = if (isSelected)
                                Color(0xFF333300)
                            else
                                Color(0xAA000000),
                            topLeft = Offset(left, top - 36f),
                            size = Size(width, 36f)
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