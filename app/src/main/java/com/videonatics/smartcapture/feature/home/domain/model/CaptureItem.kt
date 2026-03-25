package com.videonatics.smartcapture.feature.home.domain.model

data class CaptureItem(
    val id: Int,
    val name: String,
    val desc: String,
    val timestamp : Long,
    val img: String
)
