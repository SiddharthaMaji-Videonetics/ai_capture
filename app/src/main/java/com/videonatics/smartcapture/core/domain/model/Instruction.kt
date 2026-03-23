package com.videonatics.smartcapture.core.domain.model

data class Instruction(
    val moveHorizontal: Int, // -1 (left), 0, +1 (right)
    val moveVertical: Int,   // -1 (up), 0, +1 (down)
    val zoom: Int,           // +1 (zoom in), -1 (zoom out), 0
    val isPerfect: Boolean,
    val message: String
)