package com.videonatics.smartcapture.feature.imagedetails.data.datasource

import com.videonatics.smartcapture.core.network.ApiService
import com.videonatics.smartcapture.core.network.Result
import com.videonatics.smartcapture.feature.imagedetails.data.mapper.toDomain
import com.videonatics.smartcapture.feature.imagedetails.domain.model.ImageDetails
import javax.inject.Inject
import okhttp3.MultipartBody

interface ImageDetailsDataSource {
    suspend fun getImageAnalysis(data: MultipartBody.Part): Result<ImageDetails>

}

class ImageDetailsDataSourceImpl @Inject constructor(private val apiService: ApiService) :
    ImageDetailsDataSource {
    override suspend fun getImageAnalysis(data: MultipartBody.Part): Result<ImageDetails> {
        return try {
            val response = apiService.getImageAnalysis(data)
            when (response.code()) {
                200 -> {
                    if (response.body() != null) {
                        Result.Success(response.body()!!.toDomain())
                    } else {
                        Result.Error(Exception("Error: Empty response body"))
                    }
                }

                201 ->{
                    if (response.body() != null) {
                        val rp = response.body()!!.toDomain()
                        Result.Success(rp)
                    } else {
                        Result.Error(Exception("Error: Empty response body"))
                    }
                }

                else -> {
                    Result.Error(Exception("Error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

}