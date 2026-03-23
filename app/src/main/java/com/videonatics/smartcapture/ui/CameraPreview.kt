package com.videonatics.smartcapture.ui

import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.videonatics.smartcapture.core.getBoundaryRect
import com.videonatics.smartcapture.core.validateObject
import java.util.concurrent.Executors

//@TransformExperimental
//@OptIn(ExperimentalGetImage::class)
//@Composable
//fun CameraPreview(
//    onStatusUpdate: (String) -> Unit,
//    onBoxDetected: (Rect?) -> Unit
//) {
//
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val previewView = remember {
//        PreviewView(context).apply {
//            scaleType = PreviewView.ScaleType.FILL_CENTER
//        }
//    }
//
//    val detector = remember {
//
//        val options = ObjectDetectorOptions.Builder()
//            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//            .enableMultipleObjects()
//            .build()
//
//        ObjectDetection.getClient(options)
//    }
//
//    val analyzerExecutor = remember {
//        Executors.newSingleThreadExecutor()
//    }
//
//    AndroidView(
//        factory = { previewView },
//        modifier = Modifier.fillMaxSize()
//    )
//
//    LaunchedEffect(Unit) {
//
//        val cameraProvider =
//            ProcessCameraProvider.getInstance(context).get()
//
//        val preview = Preview.Builder()
//            .setTargetResolution(Size(640, 480))
//            .build()
//
//        preview.surfaceProvider = previewView.surfaceProvider
//
//        val analysis = ImageAnalysis.Builder()
//            .setTargetResolution(Size(640, 480))
//            .setBackpressureStrategy(
//                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
//            )
//            .build()
//
//        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
//
//            val mediaImage = imageProxy.image
//
//            if (mediaImage == null) {
//                imageProxy.close()
//                return@setAnalyzer
//            }
//
//            val inputImage = InputImage.fromMediaImage(
//                mediaImage,
//                imageProxy.imageInfo.rotationDegrees
//            )
//
//            detector.process(inputImage)
//
//                .addOnSuccessListener { objects ->
//
//                    val previewWidth = previewView.width
//                    val previewHeight = previewView.height
//
//                    if (previewWidth == 0 || previewHeight == 0) {
//
//                        onStatusUpdate("Preview not ready")
//                        return@addOnSuccessListener
//                    }
//
//                    val boundary =
//                        getBoundaryRect(previewWidth, previewHeight)
//
//                    if (objects.isEmpty()) {
//
//                        onStatusUpdate("No object")
//                        onBoxDetected(null)
//                        return@addOnSuccessListener
//                    }
//
//                    val obj = objects.first()
//
//                    //  USE REAL PreviewView transform
//                    val transform =
//                        previewView.outputTransform
//
//                    if (transform == null) {
//
//                        onStatusUpdate("Transform not ready")
//                        return@addOnSuccessListener
//                    }
//
//                    val rectF = RectF(obj.boundingBox)
//
//                    transform.matrix.mapRect(rectF)
//
//                    val mappedBox = Rect(
//                        rectF.left.toInt(),
//                        rectF.top.toInt(),
//                        rectF.right.toInt(),
//                        rectF.bottom.toInt()
//                    )
//
//                    // send to overlay
//                    onBoxDetected(mappedBox)
//
//                    val result =
//                        validateObject(boundary, mappedBox)
//
//                    onStatusUpdate(result)
//                }
//
//                .addOnFailureListener {
//
//                    onStatusUpdate("Detection failed")
//                    onBoxDetected(null)
//                }
//
//                .addOnCompleteListener {
//
//                    imageProxy.close()
//                }
//        }
//
//        cameraProvider.unbindAll()
//
//        cameraProvider.bindToLifecycle(
//            lifecycleOwner,
//            CameraSelector.DEFAULT_BACK_CAMERA,
//            preview,
//            analysis
//        )
//    }
//
//    DisposableEffect(Unit) {
//
//        onDispose {
//
//            analyzerExecutor.shutdown()
//            detector.close()
//        }
//    }
//}