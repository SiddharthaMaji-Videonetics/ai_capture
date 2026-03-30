package com.videonatics.smartcapture.feature.ocr.data.datasource.local

import android.graphics.Bitmap
import com.videonatics.smartcapture.core.domain.model.OcrBox
import com.videonatics.smartcapture.core.processImage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

interface OcrLocalDataSource{
    suspend fun getImageOcrBox(bitmap: Bitmap) : Flow<List<OcrBox>>
}

class OcrLocalDataSourceImpl @Inject constructor(

) : OcrLocalDataSource {
    override suspend fun getImageOcrBox(bitmap: Bitmap): Flow<List<OcrBox>> = callbackFlow {
        val result = processImage(bitmap)
        trySend(result)
        awaitClose()
    }
}