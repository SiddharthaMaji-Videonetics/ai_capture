package com.videonatics.smartcapture.imagedetails.domain.repository

import com.videonatics.smartcapture.core.network.Result
import com.videonatics.smartcapture.imagedetails.domain.model.ImageDetails
import okhttp3.MultipartBody

interface ImageDetailsRepository {
    suspend fun getImageAnalysis(data : MultipartBody.Part) : Result<ImageDetails>
}