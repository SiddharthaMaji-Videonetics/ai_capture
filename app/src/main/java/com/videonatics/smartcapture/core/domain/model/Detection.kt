package com.videonatics.smartcapture.core.domain.model

import android.graphics.RectF

data class Detection(
    val classId: Int,
    val score: Float,
    val box: RectF
)