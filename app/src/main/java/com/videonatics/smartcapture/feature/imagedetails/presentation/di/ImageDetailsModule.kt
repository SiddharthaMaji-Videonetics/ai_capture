package com.videonatics.smartcapture.feature.imagedetails.presentation.di

import com.videonatics.smartcapture.core.network.ApiService
import com.videonatics.smartcapture.feature.imagedetails.data.datasource.ImageDetailsDataSource
import com.videonatics.smartcapture.feature.imagedetails.data.datasource.ImageDetailsDataSourceImpl
import com.videonatics.smartcapture.feature.imagedetails.data.repository.ImageDetailsRepositoryImpl
import com.videonatics.smartcapture.feature.imagedetails.domain.repository.ImageDetailsRepository

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