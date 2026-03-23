package com.videonatics.smartcapture.core.data.model

data class ImageDetailsResponse(
    val date_time: String,
    val ocr: List<OcrObj>,
    val tags: List<Tag>,
    val upload_id: String
)

data class Tag(
    val confidence: Double?,
    val evaluation: Evaluation?,
    val summary: String?,
    val type: String?
)

data class Evaluation(
    val confidence: Double,
    val label: String
)

data class OcrObj(
    val bbox: List<List<Double>>,
    val confidence: Double,
    val text: String
)