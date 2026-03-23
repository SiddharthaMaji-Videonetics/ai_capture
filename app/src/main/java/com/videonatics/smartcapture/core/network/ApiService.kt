package com.videonatics.smartcapture.core.network

import com.videonatics.smartcapture.core.data.model.ImageDetailsResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("upload/photo")
    suspend fun getImageAnalysis(@Part file: MultipartBody.Part): Response<ImageDetailsResponse>
}