package com.videonatics.smartcapture.imagedetails.domain.model

import com.videonatics.smartcapture.core.data.model.Evaluation

data class ImageDetails(
    val tags: List<Pair<String, Any>>,
    val ocrText: String,
)

data class OCRText(
    val text: String,
)
