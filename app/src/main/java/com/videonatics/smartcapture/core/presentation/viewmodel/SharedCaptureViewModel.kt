package com.videonatics.smartcapture.core.presentation.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.combinedParallaxDetection
import com.videonatics.smartcapture.core.cropBoundary
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.BoundaryPercent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class SharedCaptureViewModel @Inject constructor(
    private val coreRepository: CoreRepository
) : ViewModel() {

    var capturedLocation: Pair<Double, Double>? = null
    var ocrString: String = ""
    val depthImageNoThreshold = 20

    //depth ---
    val capturedImageSeq = ArrayList<Bitmap>()
    fun setOfflineAnalysis(offlineAnalysis: Boolean) {
        coreRepository.setOfflineAnalysis(offlineAnalysis)
    }

    fun isOfflineAnalysis(): Boolean {
        return coreRepository.getOfflineAnalysis()
    }

    fun isBoundBoxEnabled(): Boolean {
        return coreRepository.getBoundBoxOption()
    }

    fun isDepthDetectionEnabled(): Boolean {
        return coreRepository.isDepthDetectionOn()
    }

    fun analyzeDepth(images: List<Bitmap>, onResult: (Boolean) -> Unit) {

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Ensure images list is not modified while processing
                val imagesCopy = images.toList()
                val isRealObj = combinedParallaxDetection(imagesCopy)
                withContext(Dispatchers.Main) {
                    onResult.invoke(isRealObj)
                }
            } catch (e: Throwable) {
                Log.e("SharedCaptureViewModel", "Error in depth analysis: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult.invoke(false)
                }
            }
        }
    }

    fun cropAndProcessImage(bitmap: Bitmap,onCroppedImg:(Bitmap)-> Unit) {
        viewModelScope.launch {
            val croppedBmp = cropBoundary(bitmap, BoundaryPercent())

            val outputStream = ByteArrayOutputStream()
            croppedBmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            onCroppedImg.invoke(croppedBmp)
        }

    }

    fun trimDepthImageSeq() {
        if (capturedImageSeq.size > depthImageNoThreshold/2) {
            capturedImageSeq.subList(0, capturedImageSeq.size - (depthImageNoThreshold/2)).clear()
        }
    }

    var capturedImage: ByteArray? = null
    var capturedFullImage: Bitmap? = null
}