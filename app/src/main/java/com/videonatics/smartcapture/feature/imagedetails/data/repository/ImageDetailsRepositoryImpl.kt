package com.videonatics.smartcapture.feature.imagedetails.data.repository

import com.videonatics.smartcapture.core.network.Result
import com.videonatics.smartcapture.feature.imagedetails.data.datasource.ImageDetailsDataSource
import com.videonatics.smartcapture.feature.imagedetails.domain.model.ImageDetails
import com.videonatics.smartcapture.feature.imagedetails.domain.repository.ImageDetailsRepository

import javax.inject.Inject
import okhttp3.MultipartBody


class ImageDetailsRepositoryImpl @Inject constructor(
    val dataSource: ImageDetailsDataSource
) : ImageDetailsRepository {
    override suspend fun getImageAnalysis(data : MultipartBody.Part) : Result<ImageDetails>{
        return dataSource.getImageAnalysis(data)

    }
}