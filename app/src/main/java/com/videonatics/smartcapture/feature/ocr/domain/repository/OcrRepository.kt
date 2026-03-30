package com.videonatics.smartcapture.feature.ocr.domain.repository

import android.graphics.Bitmap
import com.videonatics.smartcapture.core.domain.model.OcrBox
import kotlinx.coroutines.flow.Flow

interface OcrRepository {
    suspend fun getImageOcrBox(bitmap: Bitmap) : Flow<List<OcrBox>>
}