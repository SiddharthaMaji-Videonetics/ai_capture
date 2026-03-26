package com.videonatics.smartcapture.feature.capture.data.repository

import com.videonatics.smartcapture.feature.capture.data.datasource.local.CaptureLocalDatasource
import com.videonatics.smartcapture.feature.capture.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CaptureRepositoryImpl @Inject constructor(val local: CaptureLocalDatasource) : CaptureRepository {
    override fun getLocationFlow(): Flow<Pair<Double, Double>> = local.getLocationFlow()
}