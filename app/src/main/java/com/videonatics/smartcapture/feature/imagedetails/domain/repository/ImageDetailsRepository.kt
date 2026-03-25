package com.videonatics.smartcapture.feature.imagedetails.domain.repository

import com.videonatics.smartcapture.core.network.Result
import com.videonatics.smartcapture.feature.imagedetails.domain.model.ImageDetails
import okhttp3.MultipartBody

interface ImageDetailsRepository {
    suspend fun getImageAnalysis(data : MultipartBody.Part) : Result<ImageDetails>
}