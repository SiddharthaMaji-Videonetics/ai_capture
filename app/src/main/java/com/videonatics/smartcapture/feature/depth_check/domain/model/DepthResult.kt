package com.videonatics.smartcapture.feature.depth_check.domain.model

data class DepthResult(
    val isDepth: Boolean,
    val confidence: Float,
    val spatialScore: Float,
    val directionalScore: Float
)