package com.videonatics.smartcapture.feature.ocr.presentation.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.domain.model.OcrBox
import com.videonatics.smartcapture.feature.ocr.domain.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val repository: OcrRepository
) : ViewModel() {

    fun getOcrFromInputImage(bitmap: Bitmap, onOcrDetected: (List<OcrBox>) -> Unit){
        viewModelScope.launch {
            repository.getImageOcrBox(bitmap).collectLatest {
                onOcrDetected.invoke(it)
            }}
    }
}