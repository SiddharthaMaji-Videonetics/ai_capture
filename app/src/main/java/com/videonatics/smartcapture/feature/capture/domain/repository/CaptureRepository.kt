package com.videonatics.smartcapture.feature.capture.domain.repository

import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun getLocationFlow(): Flow<Pair<Double, Double>>
}