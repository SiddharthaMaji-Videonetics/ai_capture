package com.videonatics.smartcapture.core.domain.model

import android.graphics.PointF
import android.graphics.RectF


data class OcrBox(
    val id: Int,
    val text: String,
    val rect: RectF,
    val cornerPoints: List<PointF> // 4 points
)