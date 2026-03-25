package com.videonatics.smartcapture.feature.imagedetails.domain.model


data class ImageDetails(
    val tags: List<Pair<String, Any>>,
    val ocrText: String,
)

data class OCRText(
    val text: String,
)
