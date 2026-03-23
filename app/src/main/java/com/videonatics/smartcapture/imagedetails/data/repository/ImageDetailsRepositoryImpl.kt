package com.videonatics.smartcapture.imagedetails.data.repository

import com.videonatics.smartcapture.core.network.Result
import com.videonatics.smartcapture.imagedetails.data.datasource.ImageDetailsDataSource
import com.videonatics.smartcapture.imagedetails.domain.model.ImageDetails
import com.videonatics.smartcapture.imagedetails.domain.repository.ImageDetailsRepository
import javax.inject.Inject
import okhttp3.MultipartBody


class ImageDetailsRepositoryImpl @Inject constructor(
    val dataSource: ImageDetailsDataSource
) : ImageDetailsRepository{
    override suspend fun getImageAnalysis(data : MultipartBody.Part) : Result<ImageDetails>{
        return dataSource.getImageAnalysis(data)

    }
}