package com.videonatics.smartcapture.core.domain.model

import androidx.compose.ui.geometry.Rect


data class OcrBox(
    val id: Int,
    val text: String,
    val rect: Rect
)