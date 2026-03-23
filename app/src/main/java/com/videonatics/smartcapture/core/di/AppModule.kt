package com.videonatics.smartcapture.core.di

import android.content.Context
import com.videonatics.smartcapture.core.BASE_URL_PRODUCTION
import com.videonatics.smartcapture.core.PrefDelegate
import com.videonatics.smartcapture.core.data.datasource.local.CoreLocalDataSource
import com.videonatics.smartcapture.core.data.datasource.local.CoreLocalDataSourceImpl
import com.videonatics.smartcapture.core.data.repository.CoreRepositoryImpl
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.core.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providePref(@ApplicationContext context: Context): PrefDelegate {
        return PrefDelegate(context) // Return the actual implementation
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_PRODUCTION)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCoreLocalDataSource(prefDelegate: PrefDelegate): CoreLocalDataSource {
        return CoreLocalDataSourceImpl(prefDelegate)
    }

    @Provides
    @Singleton
    fun provideCoreRepository(dataSource: CoreLocalDataSource): CoreRepository {
        return CoreRepositoryImpl(dataSource)
    }

}