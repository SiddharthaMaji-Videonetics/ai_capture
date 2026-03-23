package com.videonatics.smartcapture.capture.presentation.view

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.videonatics.smartcapture.core.ARCoreManager
import com.videonatics.smartcapture.core.checkFlatnessWithARCore
import com.videonatics.smartcapture.core.cropBoundary
import com.videonatics.smartcapture.core.cropBoundingBox
import com.videonatics.smartcapture.core.ensureARCoreInstalled
import com.videonatics.smartcapture.core.getBoundaryRect
import com.videonatics.smartcapture.core.handleDetectionResult
import com.videonatics.smartcapture.core.mapPreviewRectToBitmap
import com.videonatics.smartcapture.ui.BoundaryPercent
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewHybrid(
    captureTrigger: Boolean,
    isDepthDetectionEnabled: Boolean,
    onLocalResult: (String) -> Unit,
    onFrameCaptured: (ByteArray) -> Unit,
    onFullImageCaptured: (Bitmap) -> Unit,
    onBoundingBox: (Rect) -> Unit,
    isBoundingBoxEnabled: Boolean = true,
    onArCoreNotInstalled: () -> Unit
) {
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentCaptureTrigger by rememberUpdatedState(captureTrigger)
    val currentOnLocalResult by rememberUpdatedState(onLocalResult)
    val currentOnFrameCaptured by rememberUpdatedState(onFrameCaptured)
    val currentOnFullImageCaptured by rememberUpdatedState(onFullImageCaptured)


    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    val detector = remember {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
        ObjectDetection.getClient(options)
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isCapturing = remember { AtomicBoolean(false) }
    val lastDetectionTime = remember { AtomicLong(0L) }
    val isDisposed = remember { AtomicBoolean(false) }

    val resolutionSelector = remember {
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
    }

    val preview = remember {
        Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
    }

    val analysis = remember {
        ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolutionSelector)
            .build()
    }

    LaunchedEffect(Unit) {
        if(isDepthDetectionEnabled){
            val isInstalled = ensureARCoreInstalled(context as Activity)
            if(isInstalled) {
                ARCoreManager.init(context)
                ARCoreManager.start()
                ARCoreManager.resume()
            }else{
                onArCoreNotInstalled.invoke()
            }
        }

    }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            ARCoreManager.stop()
            
            cameraExecutor.shutdown()
            try {
                val future = ProcessCameraProvider.getInstance(context)
                if (future.isDone) {
                    future.get().unbindAll()
                }
            } catch (e: Exception) {
                Log.e("CameraPreviewHybrid", "Cleanup failed", e)
            }
            detector.close()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProvider = suspendCoroutine<ProcessCameraProvider?> { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    Log.e("CameraPreviewHybrid", "Failed to get camera provider", e)
                    continuation.resume(null)
                }
            }, ContextCompat.getMainExecutor(context))
        } ?: return@LaunchedEffect

        if (isDisposed.get()) return@LaunchedEffect

        preview.surfaceProvider = previewView.surfaceProvider

        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            if (isDisposed.get()) {
                imageProxy.close()
                return@setAnalyzer
            }

            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setAnalyzer
            }

            val now = System.currentTimeMillis()
            if (now - lastDetectionTime.get() < 150) {
                imageProxy.close()
                return@setAnalyzer
            }
            lastDetectionTime.set(now)

            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            detector.process(inputImage)
                .addOnSuccessListener { objects ->
                    if (isDisposed.get()) return@addOnSuccessListener

                    val currentSize = previewSize
                    if (currentSize.width <= 0 || currentSize.height <= 0) return@addOnSuccessListener

                    val boundary = getBoundaryRect(currentSize.width, currentSize.height)

                    handleDetectionResult(
                        objects = objects,
                        boundary = boundary,
                        previewView = previewView,
                        imageWidth = inputImage.width,
                        imageHeight = inputImage.height,
                        rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                        onLocalResult = { result, box ->
                            if (isDisposed.get()) return@handleDetectionResult

                            box?.apply {

                                onBoundingBox.invoke(this)
                                if(isDepthDetectionEnabled){ //check if flatness if depth enabled
                                    val isFlat = checkFlatnessWithARCore(
                                        box = this,
                                        previewView = previewView,
                                        imageWidth = inputImage.width,
                                        imageHeight = inputImage.height
                                    )
                                }


                                if (currentCaptureTrigger && 
                                    result.contains("Perfect") && 
                                    !isDisposed.get() &&
                                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                                    isCapturing.compareAndSet(false, true)) {

                                    try {
                                        imageCapture.takePicture(
                                            cameraExecutor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    try {
                                                        val buffer = image.planes[0].buffer
                                                        val bytes = ByteArray(buffer.remaining())
                                                        buffer.get(bytes)
                                                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                        val rotationDegrees = image.imageInfo.rotationDegrees
                                                        val rotatedBitmap = if (rotationDegrees != 0) {
                                                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                                        } else {
                                                            bitmap
                                                        }
                                                        val imageRect = mapPreviewRectToBitmap(
                                                            previewRect = this@apply,
                                                            previewWidth = previewView.width,
                                                            previewHeight = previewView.height,
                                                            bitmapWidth = rotatedBitmap.width,
                                                            bitmapHeight = rotatedBitmap.height
                                                        )
                                                        val croppedBmp = if(isBoundingBoxEnabled)
                                                            cropBoundingBox(rotatedBitmap, imageRect)
                                                        else
                                                            cropBoundary(rotatedBitmap, BoundaryPercent())

                                                        val outputStream = ByteArrayOutputStream()
                                                        croppedBmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)

                                                        currentOnFrameCaptured(outputStream.toByteArray())
                                                        currentOnFullImageCaptured(rotatedBitmap)
                                                    } catch (e: Exception) {
                                                        Log.e("CameraPreviewHybrid", "Capture processing failed", e)
                                                    } finally {
                                                        isCapturing.set(false)
                                                        image.close()
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    Log.e("CameraPreviewHybrid", "Capture failed", exception)
                                                    isCapturing.set(false)
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Log.e("CameraPreviewHybrid", "takePicture call failed", e)
                                        isCapturing.set(false)
                                    }
                                }
                            }
                            currentOnLocalResult(result)
                        }
                    )
                }
                .addOnFailureListener {
                    Log.e("ML", "Detection failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraPreviewHybrid", "Binding failed", e)
        }
    }

    LaunchedEffect(captureTrigger) {
        if (!captureTrigger) {
            isCapturing.set(false)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { previewSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    val cam = camera ?: return@detectTransformGestures
                    val zoomState = cam.cameraInfo.zoomState.value
                    if(zoomState !=null){
                        val clampedZoom = (zoomState.zoomRatio * zoom).coerceIn(
                            zoomState.minZoomRatio,
                            zoomState.maxZoomRatio
                        )
                        cam.cameraControl.setZoomRatio(clampedZoom)
                        zoomRatio = clampedZoom
                    }
                }
            }
    )
}
