package com.videonatics.smartcapture.capture.domain.model

data class Tilt(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isLevel: Boolean = false,
    val isStable: Boolean = false
)