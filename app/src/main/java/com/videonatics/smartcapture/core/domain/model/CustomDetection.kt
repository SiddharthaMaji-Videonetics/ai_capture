package com.videonatics.smartcapture.core.domain.model

import android.graphics.RectF

data class CustomDetection(
    val box: RectF,
    val confidence: Float,
    val label: String
)