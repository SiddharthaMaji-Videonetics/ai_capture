package com.videonatics.smartcapture.core

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import android.widget.Toast

import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.Color
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.domain.model.Instruction
import com.videonatics.smartcapture.core.domain.model.OcrBox
import com.videonatics.smartcapture.feature.capture.domain.model.ARFrameHolder
import com.videonatics.smartcapture.ui.BoundaryPercent
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import kotlin.collections.map
import kotlin.coroutines.resumeWithException
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

//depth process for image
fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun extractDepth(depthImage: Image): ShortArray {
    val buffer = depthImage.planes[0].buffer
    val depthArray = ShortArray(buffer.remaining() / 2)
    buffer.asShortBuffer().get(depthArray)
    return depthArray
}

fun centerROI(depth: ShortArray, width: Int, height: Int): List<Int> {
    val result = mutableListOf<Int>()

    val startX = width / 4
    val endX = 3 * width / 4
    val startY = height / 4
    val endY = 3 * height / 4

    for (y in startY until endY) {
        for (x in startX until endX) {
            val value = depth[y * width + x].toInt()
            if (value > 0) result.add(value)
        }
    }
    return result
}

fun computeRange(values: List<Int>): Int {
    if (values.isEmpty()) return 0
    return values.maxOrNull()!! - values.minOrNull()!!
}

fun calculateAverageFlow(frames: List<Mat>): Float {
    var total = 0.0

    for (i in 0 until frames.size - 1) {
        val flow = Mat()

        Video.calcOpticalFlowFarneback(
            frames[i], frames[i + 1], flow,
            0.5, 3, 15, 3, 5, 1.2, 0
        )

        val xy = ArrayList<Mat>()
        Core.split(flow, xy)

        val mag = Mat()
        Core.magnitude(xy[0], xy[1], mag)

        total += Core.mean(mag).`val`[0]
    }

    return (total / (frames.size - 1)).toFloat()
}

val confidenceHistory = ArrayDeque<Float>()
val directionalHistory = ArrayDeque<Float>()

val MAX_HISTORY = 5
fun detectDepth(bitmaps: List<Bitmap>): Boolean {

    val frames = preprocess(bitmaps)

    if (!isSequenceValid(frames)) return false

    val motion = calculateAverageFlow(frames)
    if (motion < 0.05f || motion > 15f) return false

    val spatial = calculateSpatialDepth(frames)
    val directional = calculateDirectionalVariance(frames)

    val spatialNorm = (spatial / 2f).coerceIn(0f, 1f)
    val directionalNorm = directional.coerceIn(0f, 1f)

    val directionalBoost =
        Math.pow(directionalNorm.toDouble(), 1.7).toFloat()

    val confidence =
        0.3f * spatialNorm +
                0.7f * directionalBoost

    return updateDepth(confidence, directional)
}

 fun preprocess(bitmaps: List<Bitmap>): List<Mat> {
    return bitmaps.map {
        val gray = bitmapToGrayMat(it)
        resizeMat(gray)
    }
}

private fun resizeMat(src: Mat, width: Int = 320, height: Int = 240): Mat {
    val dst = Mat()
    Imgproc.resize(src, dst, Size(width.toDouble(), height.toDouble()))
    return dst
}

private fun getMatchedPoints(prev: Mat, next: Mat): Pair<List<org.opencv.core.Point>, List<org.opencv.core.Point>> {

    val orb = ORB.create(1000)

    val kp1 = MatOfKeyPoint()
    val kp2 = MatOfKeyPoint()
    val desc1 = Mat()
    val desc2 = Mat()

    orb.detectAndCompute(prev, Mat(), kp1, desc1)
    orb.detectAndCompute(next, Mat(), kp2, desc2)

    if (desc1.empty() || desc2.empty()) {
        return Pair(emptyList(), emptyList())
    }

    val matcher = BFMatcher.create(Core.NORM_HAMMING, false)
    val matches = ArrayList<MatOfDMatch>()
    matcher.knnMatch(desc1, desc2, matches, 2)

    val goodPts1 = mutableListOf<org.opencv.core.Point>()
    val goodPts2 = mutableListOf<org.opencv.core.Point>()

    val kp1List = kp1.toArray()
    val kp2List = kp2.toArray()

    for (m in matches) {
        val pair = m.toArray()
        if (pair.size >= 2 && pair[0].distance < 0.75 * pair[1].distance) {
            val p1 = kp1List[pair[0].queryIdx].pt
            val p2 = kp2List[pair[0].trainIdx].pt

            goodPts1.add(p1)
            goodPts2.add(p2)
        }
    }

    return Pair(goodPts1, goodPts2)
}

private fun bitmapToGrayMat(bitmap: Bitmap): Mat {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)

    val gray = Mat()
    Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

   //flow stability
    Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

    return gray
}
private fun isSequenceValid(frames: List<Mat>): Boolean {

    for (i in 0 until frames.size - 1) {

        val (pts1, pts2) = getMatchedPoints(frames[i], frames[i + 1])

        if (pts1.size < 20) return false

        val mat1 = MatOfPoint2f(*pts1.toTypedArray())
        val mat2 = MatOfPoint2f(*pts2.toTypedArray())

        val mask = Mat()

        Calib3d.findHomography(
            mat1, mat2,
            Calib3d.RANSAC,
            3.0,
            mask
        )

        val inliers = Core.countNonZero(mask)
        val ratio = inliers.toFloat() / pts1.size

        if (ratio < 0.3f) return false
    }

    return true
}

private fun calculateSpatialDepth(frames: List<Mat>): Float {
    if (frames.size < 2) return 0f

    var total = 0.0

    for (i in 0 until frames.size - 1) {

        val flow = Mat()
        Video.calcOpticalFlowFarneback(
            frames[i], frames[i + 1], flow,
            0.5, 3, 15, 3, 5, 1.2, 0
        )

        val xy = ArrayList<Mat>()
        Core.split(flow, xy)

        val mag = Mat()
        Core.magnitude(xy[0], xy[1], mag)

        //  LIGHT global motion removal (not full subtract)
        val meanVal = Core.mean(mag).`val`[0]
        val centered = Mat()
        Core.subtract(mag, Scalar(meanVal * 0.7), centered)

        Core.max(centered, Scalar(0.0), centered)

        //LOWER threshold (important fix)
        val maskFloat = Mat()
        Imgproc.threshold(centered, maskFloat, 0.03, 1.0, Imgproc.THRESH_BINARY)

    // Convert to 8-bit mask
        val mask = Mat()
        maskFloat.convertTo(mask, CvType.CV_8U)

        val filtered = Mat()
        centered.copyTo(filtered, mask)

        val mean = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(filtered, mean, std)

        // REMOVE normalization (critical change)
        val stdVal = std.toArray()[0]

// normalize
        val normalized = stdVal / meanVal

// clamp extreme values
        val stable = normalized.coerceIn(0.0, 2.0)

        total += stable
    }

    return (total / (frames.size - 1)).toFloat()
}

private fun calculateDirectionalVariance(frames: List<Mat>): Float {
    if (frames.size < 2) return 0f

    var total = 0.0

    for (i in 0 until frames.size - 1) {

        val flow = Mat()
        Video.calcOpticalFlowFarneback(
            frames[i], frames[i + 1], flow,
            0.5, 3, 15, 3, 5, 1.2, 0
        )

        val xy = ArrayList<Mat>()
        Core.split(flow, xy)

        val mag = Mat()
        val angle = Mat()
        Core.cartToPolar(xy[0], xy[1], mag, angle, true)

        // LOWER threshold
        val mask = Mat()
        // create proper binary mask
        Imgproc.threshold(mag, mask, 0.03, 255.0, Imgproc.THRESH_BINARY)

// convert to 8-bit (required by copyTo)
        mask.convertTo(mask, CvType.CV_8U)

        val x = Mat()
        val y = Mat()
        Core.polarToCart(mag, angle, x, y, true)

        val xMasked = Mat()
        val yMasked = Mat()
        x.copyTo(xMasked, mask)
        y.copyTo(yMasked, mask)

        val meanX = Core.mean(xMasked).`val`[0]
        val meanY = Core.mean(yMasked).`val`[0]

        val magMean = Math.sqrt(meanX * meanX + meanY * meanY)

        val varScore = 1.0 - (magMean / (Core.mean(mag).`val`[0] + 1e-5))

        total += varScore
    }

    return (total / (frames.size - 1)).toFloat()
}

private fun calculateFlowConsistency(frames: List<Mat>): Float {
    var total = 0.0

    for (i in 0 until frames.size - 1) {

        val flow = Mat()
        Video.calcOpticalFlowFarneback(
            frames[i], frames[i + 1], flow,
            0.5, 3, 15, 3, 5, 1.2, 0
        )

        val xy = ArrayList<Mat>()
        Core.split(flow, xy)

        val meanX = Core.mean(xy[0]).`val`[0]
        val meanY = Core.mean(xy[1]).`val`[0]

        val consistency = Math.sqrt(meanX * meanX + meanY * meanY)

        total += consistency
    }

    return (total / (frames.size - 1)).toFloat()
}

private fun calculateEdgeDensity(frame: Mat): Float {
    val edges = Mat()
    Imgproc.Canny(frame, edges, 50.0, 150.0)

    val edgePixels = Core.countNonZero(edges)
    val totalPixels = frame.rows() * frame.cols()

    return edgePixels.toFloat() / totalPixels
}

fun updateDepth(confidence: Float, directional: Float): Boolean {

    //  ALWAYS store
    confidenceHistory.addLast(confidence)
    directionalHistory.addLast(directional)

    if (confidenceHistory.size > MAX_HISTORY) {
        confidenceHistory.removeFirst()
        directionalHistory.removeFirst()
    }

    //  allow faster activation
    if (directionalHistory.size < 2) return false

    val strongDirectional = directionalHistory.filter { it > 0.10f }
    val strongConfidence = confidenceHistory.zip(directionalHistory)
        .filter { it.second > 0.10f }
        .map { it.first }

    //  require at least 2 strong frames
    if (strongDirectional.size < 2) return false

    val avgDirectional = strongDirectional.average().toFloat()
    val avgConfidence = strongConfidence.average().toFloat()

    val result =
        avgDirectional > 0.12f &&
                avgConfidence > 0.23f

    Log.d(
        "DEPTH_TEMPORAL",
        """
        raw_dir=$directionalHistory
        strong_dir=$strongDirectional
        avgDir=$avgDirectional
        avgConf=$avgConfidence
        FINAL=$result
        """.trimIndent()
    )

    return result
}

suspend fun processImageForOCR(bitmap: Bitmap): List<OcrBox> =
    suspendCancellableCoroutine { cont ->

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->

                val results = mutableListOf<OcrBox>()
                var idCounter = 0

                val w = bitmap.width.toFloat()
                val h = bitmap.height.toFloat()

                fun addBox(
                    text: String,
                    rect: Rect,
                    points: Array<Point>
                ) {
                    if (points.size != 4) return

                    results.add(
                        OcrBox(
                            id = idCounter++,
                            text = text,
                            rect = RectF(
                                rect.left / w,
                                rect.top / h,
                                rect.right / w,
                                rect.bottom / h
                            ),
                            cornerPoints = points.map {
                                PointF(it.x / w, it.y / h)
                            }
                        )
                    )
                }

                visionText.textBlocks.forEach { block ->

                    var hasAny = false

                    block.lines.forEach { line ->

                        var lineHasElements = false

                        line.elements.forEach { element ->
                            val rect = element.boundingBox
                            val points = element.cornerPoints

                            if (rect != null && points != null) {
                                addBox(element.text, rect, points)
                                lineHasElements = true
                                hasAny = true
                            }
                        }

                        if (!lineHasElements) {
                            val rect = line.boundingBox
                            val points = line.cornerPoints

                            if (rect != null && points != null) {
                                addBox(line.text, rect, points)
                                hasAny = true
                            }
                        }
                    }

                    if (!hasAny) {
                        val rect = block.boundingBox
                        val points = block.cornerPoints

                        if (rect != null && points != null) {
                            addBox(block.text, rect, points)
                        }
                    }
                }

                if (cont.isActive) cont.resume(results, null)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resumeWithException(it)
            }
    }
