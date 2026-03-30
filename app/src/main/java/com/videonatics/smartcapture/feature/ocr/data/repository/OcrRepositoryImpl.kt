package com.videonatics.smartcapture.feature.ocr.data.repository

import android.graphics.Bitmap
import com.videonatics.smartcapture.core.domain.model.OcrBox
import com.videonatics.smartcapture.feature.ocr.data.datasource.local.OcrLocalDataSource
import com.videonatics.smartcapture.feature.ocr.domain.repository.OcrRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OcrRepositoryImpl @Inject constructor(
    private val local: OcrLocalDataSource
) : OcrRepository {
    override suspend fun getImageOcrBox(bitmap: Bitmap): Flow<List<OcrBox>> = local.getImageOcrBox(bitmap)
}