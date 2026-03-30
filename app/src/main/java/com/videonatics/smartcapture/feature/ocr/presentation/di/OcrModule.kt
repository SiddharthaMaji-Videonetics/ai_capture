package com.videonatics.smartcapture.feature.ocr.presentation.di

import com.videonatics.smartcapture.feature.ocr.data.datasource.local.OcrLocalDataSource
import com.videonatics.smartcapture.feature.ocr.data.datasource.local.OcrLocalDataSourceImpl
import com.videonatics.smartcapture.feature.ocr.data.repository.OcrRepositoryImpl
import com.videonatics.smartcapture.feature.ocr.domain.repository.OcrRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OcrModule {
    @Provides
    @Singleton
    fun provideOcrLocalDataSource(): OcrLocalDataSource {
        return OcrLocalDataSourceImpl()
    }

    @Provides
    @Singleton
    fun provideOcrRepository(local : OcrLocalDataSource): OcrRepository {
        return OcrRepositoryImpl(local)
    }
}
