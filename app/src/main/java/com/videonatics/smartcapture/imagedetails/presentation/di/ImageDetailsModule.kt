package com.videonatics.smartcapture.imagedetails.presentation.di

import com.videonatics.smartcapture.core.network.ApiService
import com.videonatics.smartcapture.imagedetails.data.datasource.ImageDetailsDataSource
import com.videonatics.smartcapture.imagedetails.data.datasource.ImageDetailsDataSourceImpl
import com.videonatics.smartcapture.imagedetails.data.repository.ImageDetailsRepositoryImpl
import com.videonatics.smartcapture.imagedetails.domain.repository.ImageDetailsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageDetailsModule {

    @Provides
    @Singleton
    fun provideImageDetailsDataSource(apiService: ApiService): ImageDetailsDataSource {
        return ImageDetailsDataSourceImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideImageDetailsRepository(dataSource: ImageDetailsDataSource): ImageDetailsRepository {
        return ImageDetailsRepositoryImpl(dataSource)

    }
}