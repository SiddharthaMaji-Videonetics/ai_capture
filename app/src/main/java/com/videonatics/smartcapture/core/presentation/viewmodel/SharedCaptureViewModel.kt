package com.videonatics.smartcapture.core.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedCaptureViewModel @Inject constructor(
    private val coreRepository: CoreRepository
) : ViewModel() {
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

    var capturedImage: ByteArray? = null
    var capturedFullImage: Bitmap? = null
}