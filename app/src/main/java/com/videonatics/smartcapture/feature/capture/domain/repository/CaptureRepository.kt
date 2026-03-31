package com.videonatics.smartcapture.feature.capture.domain.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun getLocationFlow(): Flow<Location>
}