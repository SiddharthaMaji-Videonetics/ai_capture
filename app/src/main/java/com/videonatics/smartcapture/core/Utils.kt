package com.videonatics.smartcapture.core

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.compose.ui.graphics.Color
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.videonatics.smartcapture.capture.domain.model.ARFrameHolder
import com.videonatics.smartcapture.core.ar_core.ARCoreManager
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.domain.model.Instruction
import com.videonatics.smartcapture.ui.BoundaryPercent
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt


fun getBoundaryRect(
    previewWidth: Int,
    previewHeight: Int
): Rect {

    val left = (previewWidth * 0.2f).toInt()
    val top = (previewHeight * 0.3f).toInt()
    val right = (previewWidth * 0.8f).toInt()
    val bottom = (previewHeight * 0.7f).toInt()

    return Rect(left, top, right, bottom)
}
fun mapBoundingBoxCenterCrop(
    boundingBox: Rect,
    imageWidth: Int,
    imageHeight: Int,
    previewWidth: Int,
    previewHeight: Int
): Rect {

    val scale =
        maxOf(
            previewWidth.toFloat() / imageWidth,
            previewHeight.toFloat() / imageHeight
        )

    val scaledWidth = imageWidth * scale
    val scaledHeight = imageHeight * scale

    val dx = (scaledWidth - previewWidth) / 2f
    val dy = (scaledHeight - previewHeight) / 2f

    val left = boundingBox.left * scale - dx
    val top = boundingBox.top * scale - dy
    val right = boundingBox.right * scale - dx
    val bottom = boundingBox.bottom * scale - dy

    return Rect(
        left.toInt().coerceIn(0, previewWidth),
        top.toInt().coerceIn(0, previewHeight),
        right.toInt().coerceIn(0, previewWidth),
        bottom.toInt().coerceIn(0, previewHeight)
    )
}



private const val TAG = "BOUNDARY_DEBUG"

fun handleDetectionResult(
    objects: List<DetectedObject>,
    boundary: Rect,
    previewView: PreviewView,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    onLocalResult: (Instruction, Rect?) -> Unit
) {

    val previewWidth = previewView.width
    val previewHeight = previewView.height

    Log.d(TAG, "---------- NEW FRAME ----------")
    Log.d(TAG, "Preview: $previewWidth x $previewHeight")
    Log.d(TAG, "Image: $imageWidth x $imageHeight")
    Log.d(TAG, "Rotation: $rotationDegrees")
    Log.d(TAG, "Boundary: $boundary")

    if (previewWidth == 0 || previewHeight == 0) {
        Log.d(TAG, "Preview not ready")
        onLocalResult(Instruction(
            moveHorizontal = 0,
            moveVertical = 0,
            zoom = 0,
            isPerfect = false,
            "Preview not ready"
        ),null)
        return
    }

    if (objects.isEmpty()) {
        Log.d(TAG, "No objects detected")
        onLocalResult(Instruction(
            moveHorizontal = 0,
            moveVertical = 0,
            zoom = 0,
            isPerfect = false,
            "Searching..."
        ), null)
        return
    }

    val rotatedWidth: Int
    val rotatedHeight: Int

    if (rotationDegrees == 90 || rotationDegrees == 270) {
        rotatedWidth = imageHeight
        rotatedHeight = imageWidth
    } else {
        rotatedWidth = imageWidth
        rotatedHeight = imageHeight
    }

    Log.d(TAG, "Rotated Image: $rotatedWidth x $rotatedHeight")

    val mappedObjects =
        objects.mapIndexed { index, obj ->

            val confidence =
                obj.labels.firstOrNull()?.confidence ?: 0f

            Log.d(TAG, "Object[$index] original: ${obj.boundingBox}")
            Log.d(TAG, "Object[$index] confidence: $confidence")

            val mapped =
                mapBoundingBoxCenterCrop(
                    obj.boundingBox,
                    rotatedWidth,
                    rotatedHeight,
                    previewWidth,
                    previewHeight
                )

            Log.d(TAG, "Object[$index] mapped: $mapped")

            val overlap =
                calculateOverlap(boundary, mapped)

            Log.d(TAG, "Object[$index] overlap: ${(overlap * 100).toInt()}%")

            Pair(obj, mapped)
        }

    val filtered =
        mappedObjects.filter {

            val overlap =
                calculateOverlap(boundary, it.second)

            overlap >= 0.6f
        }

    if (filtered.isEmpty()) {

        Log.d(TAG, "RESULT: Object outside boundary")

        onLocalResult(Instruction(
            moveHorizontal = 0,
            moveVertical = 0,
            zoom = 0,
            isPerfect = false,
            "Object outside boundary"
        ), null)
        return
    }

    val best =
        filtered.maxByOrNull {
            it.second.width() * it.second.height()
        }

    best?.let {

        val label =
            it.first.labels.firstOrNull()?.text ?: "Object"

        val framing =
            validateObject(boundary, it.second)

        Log.d(TAG, "RESULT: $label : $framing")

        onLocalResult(framing, it.second)
    }
}




fun imageProxyToByteArray(image: ImageProxy): ByteArray {

    val width = image.width
    val height = image.height

    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val yRowStride = yPlane.rowStride
    val uvRowStride = uPlane.rowStride
    val uvPixelStride = uPlane.pixelStride

    val nv21 = ByteArray(width * height * 3 / 2)

    // Copy Y correctly (respect rowStride)
    var outputOffset = 0
    for (row in 0 until height) {
        yBuffer.position(row * yRowStride)
        yBuffer.get(nv21, outputOffset, width)
        outputOffset += width
    }

    // Copy UV correctly (interleave VU for NV21)
    val chromaHeight = height / 2
    val chromaWidth = width / 2

    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {

            val uvOffset = row * uvRowStride + col * uvPixelStride

            nv21[outputOffset++] = vBuffer.get(uvOffset)
            nv21[outputOffset++] = uBuffer.get(uvOffset)
        }
    }

    // Convert to JPEG
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 95, out)

    return out.toByteArray()
}

fun validateObject(
    boundary: Rect,
    objectRect: Rect
): Instruction {

    val objectCenterX = objectRect.centerX()
    val objectCenterY = objectRect.centerY()

    val boundaryCenterX = boundary.centerX()
    val boundaryCenterY = boundary.centerY()

    val dx = objectCenterX - boundaryCenterX
    val dy = objectCenterY - boundaryCenterY

    val thresholdX = (boundary.width() * 0.1f).toInt()
    val thresholdY = (boundary.height() * 0.1f).toInt()

    // -------------------------------
    // POSITION (STEP BASED)
    // -------------------------------
    val moveHorizontal = when {
        dx < -thresholdX -> +1   // move right
        dx > thresholdX -> -1    // move left
        else -> 0
    }

    val moveVertical = when {
        dy < -thresholdY -> +1   // move down
        dy > thresholdY -> -1    // move up
        else -> 0
    }

    // -------------------------------
    // SIZE (ZOOM)
    // -------------------------------
    val boundaryArea = boundary.width() * boundary.height()
    val objectArea = objectRect.width() * objectRect.height()

    // avoid float if you want pure int math (optional)
    val sizeRatio = objectArea.toFloat() / boundaryArea.toFloat()

    val zoom = when {
        sizeRatio < 0.3f -> +1   // zoom in
        sizeRatio > 0.8f -> -1   // zoom out
        else -> 0
    }

    // -------------------------------
    // MESSAGE
    // -------------------------------
    val messageParts = mutableListOf<String>()

    if (moveHorizontal == -1) messageParts.add("Move Left")
    if (moveHorizontal == +1) messageParts.add("Move Right")

    if (moveVertical == -1) messageParts.add("Move Up")
    if (moveVertical == +1) messageParts.add("Move Down")

    if (zoom == +1) messageParts.add("Move closer")
    if (zoom == -1) messageParts.add("Move away")

    val isPerfect =
        moveHorizontal == 0 &&
                moveVertical == 0 &&
                zoom == 0

    val message =
        if (isPerfect) "Perfect"
        else messageParts.joinToString(", ")

    return Instruction(
        moveHorizontal = moveHorizontal,
        moveVertical = moveVertical,
        zoom = zoom,
        isPerfect = isPerfect,
        message = message
    )
}

fun isRectInsideBoundary(
    boundary: Rect,
    objectRect: Rect,
    requiredOverlap: Float = 0.6f
): Boolean {

    val intersection = Rect()

    val intersects =
        intersection.setIntersect(boundary, objectRect)

    if (!intersects) return false

    val intersectionArea =
        intersection.width() * intersection.height()

    val objectArea =
        objectRect.width() * objectRect.height()

    val overlap =
        intersectionArea.toFloat() / objectArea.toFloat()

    return overlap >= requiredOverlap
}

fun calculateOverlap(
    boundary: Rect,
    objectRect: Rect
): Float {

    val intersection = Rect()

    val intersects =
        intersection.setIntersect(boundary, objectRect)

    if (!intersects) return 0f

    val intersectionArea =
        intersection.width() * intersection.height()

    val objectArea =
        objectRect.width() * objectRect.height()

    if (objectArea == 0) return 0f

    return intersectionArea.toFloat() / objectArea.toFloat()
}

fun Float.toPercentageString(): String {
    val percent = (this * 100).coerceIn(0f, 100f)
    return "${percent.roundToInt()}%"
}

fun Float.toConfidenceColor(): Color {
    val normalized = ((this - 0.5f) / 0.5f)
        .coerceIn(0f, 1f)

    // Hue: 10° (soft red) → 130° (deep green)
    val hue = 10f + (normalized * 120f)

    return Color.hsl(
        hue = hue,
        saturation = 0.75f,   // softer than 0.9
        lightness = 0.45f     // darker for white bg contrast
    )
}

fun bitmapToTensor(bitmap: Bitmap): TensorBuffer {

    val tensorImage = TensorImage(DataType.FLOAT32)
    tensorImage.load(bitmap)

    val resized = ImageProcessor.Builder()
        .add(ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()
        .process(tensorImage)

    return resized.tensorBuffer
}

fun parseYOLO(
    outputs: FloatArray,
    imageWidth: Int,
    imageHeight: Int
): List<Detection> {

    val detections = mutableListOf<Detection>()

    val numPredictions = 8400
    val numClasses = 80
    
    // YOLOv8 output is typically [1, 84, 8400], which is transposed in the flat array.
    // 84 = 4 coordinates (cx, cy, w, h) + 80 class scores.
    // Total length: 84 * 8400 = 705,600.

    for (i in 0 until numPredictions) {

        var maxScore = 0f
        var maxClass = -1

        for (c in 0 until numClasses) {
            // Index for class scores: i + numPredictions * (c + 4)
            val score = outputs[i + numPredictions * (c + 4)]
            if (score > maxScore) {
                maxScore = score
                maxClass = c
            }
        }

        if (maxScore < 0.45f) continue

        val cx = outputs[i]
        val cy = outputs[i + numPredictions]
        val w = outputs[i + numPredictions * 2]
        val h = outputs[i + numPredictions * 3]

        // Scale from 640x640 (model input) to original image size
        val left = (cx - w / 2f) * (imageWidth / 640f)
        val top = (cy - h / 2f) * (imageHeight / 640f)
        val right = (cx + w / 2f) * (imageWidth / 640f)
        val bottom = (cy + h / 2f) * (imageHeight / 640f)

        detections.add(
            Detection(
                maxClass,
                maxScore,
                RectF(left, top, right, bottom)
            )
        )
    }

    return detections
}

fun cropBoundary(
    bitmap: Bitmap,
    boundaryPercent: BoundaryPercent
): Bitmap {

    val left = (bitmap.width * boundaryPercent.left).toInt()
    val top = (bitmap.height * boundaryPercent.top).toInt()
    val right = (bitmap.width * boundaryPercent.right).toInt()
    val bottom = (bitmap.height * boundaryPercent.bottom).toInt()

    val width = right - left
    val height = bottom - top

    return Bitmap.createBitmap(
        bitmap,
        left,
        top,
        width,
        height
    )
}

fun cropBoundingBox(bitmap: Bitmap, rect: Rect): Bitmap {

    val left = rect.left.coerceAtLeast(0)
    val top = rect.top.coerceAtLeast(0)
    val right = rect.right.coerceAtMost(bitmap.width)
    val bottom = rect.bottom.coerceAtMost(bitmap.height)

    val width = right - left
    val height = bottom - top

    if (width <= 0 || height <= 0) {
        return bitmap
    }

    return Bitmap.createBitmap(
        bitmap,
        left,
        top,
        width,
        height
    )
}

fun mapPreviewRectToBitmap(
    previewRect: Rect,
    previewWidth: Int,
    previewHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int
): Rect {

    val scaleX = bitmapWidth.toFloat() / previewWidth
    val scaleY = bitmapHeight.toFloat() / previewHeight

    return Rect(
        (previewRect.left * scaleX).toInt(),
        (previewRect.top * scaleY).toInt(),
        (previewRect.right * scaleX).toInt(),
        (previewRect.bottom * scaleY).toInt()
    )
}

fun bitmapToMultipart(bitmap: Bitmap): MultipartBody.Part {

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)

    val byteArray = stream.toByteArray()

    val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

    return MultipartBody.Part.createFormData(
        "file",        // key expected by server
        "image.jpg",   // filename
        requestBody
    )
}

fun checkFlatnessWithARCore(
    box: Rect,
    previewView: PreviewView,
    imageWidth: Int,
    imageHeight: Int
): Boolean {

    val frame = ARFrameHolder.latestFrame ?: return false
    val planes = frame.getUpdatedTrackables(Plane::class.java)
    Log.d("AR_DEBUG", "planes=${planes.size}")

    if (frame.camera.trackingState != TrackingState.TRACKING) {
        return false
    }

    // Correct mapping
    val (cx, cy) = mapToPreviewView(
        box,
        previewView,
        imageWidth,
        imageHeight
    )

    val hits = frame.hitTest(cx, cy)

    Log.d("AR_DEBUG", "cx=$cx cy=$cy hits=${hits.size}")

    return hits.any { hit ->
        val t = hit.trackable
        t is Plane &&
                t.trackingState == TrackingState.TRACKING &&
                t.isPoseInPolygon(hit.hitPose)
    }
}

fun mapToPreviewView(
    box: Rect,
    previewView: PreviewView,
    imageWidth: Int,
    imageHeight: Int
): Pair<Float, Float> {

    val viewWidth = previewView.width.toFloat()
    val viewHeight = previewView.height.toFloat()

    val imageAspect = imageWidth.toFloat() / imageHeight
    val viewAspect = viewWidth / viewHeight

    var scaleX: Float
    var scaleY: Float
    var offsetX = 0f
    var offsetY = 0f

    if (viewAspect > imageAspect) {
        // Image scaled by height (cropped left/right)
        scaleY = viewHeight / imageHeight
        scaleX = scaleY

        val scaledWidth = imageWidth * scaleX
        offsetX = (viewWidth - scaledWidth) / 2f

    } else {
        // Image scaled by width (cropped top/bottom)
        scaleX = viewWidth / imageWidth
        scaleY = scaleX

        val scaledHeight = imageHeight * scaleY
        offsetY = (viewHeight - scaledHeight) / 2f
    }

    val cx = box.centerX() * scaleX + offsetX
    val cy = box.centerY() * scaleY + offsetY

    return cx to cy
}

fun ensureARCoreInstalled(activity: Activity): Boolean {

    return try {
        val installStatus = ArCoreApk.getInstance().requestInstall(
            activity,
            true // show install dialog
        )

        when (installStatus) {
            ArCoreApk.InstallStatus.INSTALLED -> true
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> false
        }

    } catch (e: Exception) {
        false
    }
}


fun isLowLight(lux: Float): Boolean {
    return lux < 30f
}


private fun isFlatUsingDepth(frame: Frame): Boolean {

    val depthImage = try {
        frame.acquireDepthImage16Bits()
    } catch (e: Exception) {
        Log.d("Depth", "No depth → assume flat")
        return true
    }

    val buffer = depthImage.planes[0].buffer.asShortBuffer()
    val width = depthImage.width
    val height = depthImage.height

    val samples = mutableListOf<Int>()

    for (y in 0 until height step 10) {
        for (x in 0 until width step 10) {
            val index = y * width + x
            if (index < buffer.limit()) {
                val depth = buffer.get(index).toInt()
                if (depth > 0) samples.add(depth)
            }
        }
    }

    depthImage.close()

    if (samples.size < 30) {
        Log.d("Depth", "Too few samples → flat")
        return true
    }

    val mean = samples.average()
    val variance = samples.sumOf { (it - mean) * (it - mean) } / samples.size

    Log.d("Depth", "variance = $variance")

    return variance < 10000
}

fun isSceneFlat(frame: Frame): Boolean {

    if (frame.camera.trackingState != TrackingState.TRACKING) {
        return false
    }

    return isFlatUsingDepth(frame)
}

fun isFlatInBoundary(
    frame: Frame,
    boundary: BoundaryPercent
): Boolean {

    val depthImage = try {
        frame.acquireDepthImage16Bits()
    } catch (e: Exception) {
        return true // no depth → assume flat
    }

    val buffer = depthImage.planes[0].buffer.asShortBuffer()
    val depthWidth = depthImage.width
    val depthHeight = depthImage.height

    //  Step 1: boundary → normalized
    val input = floatArrayOf(
        boundary.left, boundary.top,
        boundary.right, boundary.top,
        boundary.right, boundary.bottom,
        boundary.left, boundary.bottom
    )

    val output = FloatArray(8)

    //  Step 2: map to camera texture space
    frame.transformCoordinates2d(
        Coordinates2d.VIEW_NORMALIZED,
        input,
        Coordinates2d.TEXTURE_NORMALIZED,
        output
    )

    //  Step 3: convert to pixel space
    val xs = listOf(output[0], output[2], output[4], output[6])
    val ys = listOf(output[1], output[3], output[5], output[7])

    val minX = (xs.min() * depthWidth).toInt().coerceIn(0, depthWidth - 1)
    val maxX = (xs.max() * depthWidth).toInt().coerceIn(0, depthWidth - 1)

    val minY = (ys.min() * depthHeight).toInt().coerceIn(0, depthHeight - 1)
    val maxY = (ys.max() * depthHeight).toInt().coerceIn(0, depthHeight - 1)

    val samples = mutableListOf<Int>()

    for (y in minY until maxY step 4) {
        for (x in minX until maxX step 4) {

            val index = y * depthWidth + x
            if (index < buffer.limit()) {
                val depth = buffer.get(index).toInt()

                if (depth > 0) {
                    samples.add(depth)
                }
            }
        }
    }

    depthImage.close()

    if (samples.size < 20) return true

    val mean = samples.average()
    val variance = samples.sumOf { (it - mean) * (it - mean) } / samples.size

    return variance < 8000
}