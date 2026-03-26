package com.videonatics.smartcapture.feature.capture.presentation.di

import android.content.Context
import com.videonatics.smartcapture.feature.capture.data.datasource.local.CaptureLocalDatasource
import com.videonatics.smartcapture.feature.capture.data.datasource.local.CaptureLocalDatasourceImpl
import com.videonatics.smartcapture.feature.capture.data.repository.CaptureRepositoryImpl
import com.videonatics.smartcapture.feature.capture.domain.repository.CaptureRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CaptureModule {

    @Provides
    @Singleton
    fun provideCaptureLocalDataSource(@ApplicationContext context: Context): CaptureLocalDatasource {
        return CaptureLocalDatasourceImpl(context)
    }

    @Provides
    @Singleton
    fun provideCaptureRepository(local : CaptureLocalDatasource): CaptureRepository {
        return CaptureRepositoryImpl(local)
    }

}