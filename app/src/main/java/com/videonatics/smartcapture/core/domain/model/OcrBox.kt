package com.videonatics.smartcapture.core.domain.model

import android.graphics.RectF


data class OcrBox(
    val id: Int,
    val text: String,
    val rect: RectF
)