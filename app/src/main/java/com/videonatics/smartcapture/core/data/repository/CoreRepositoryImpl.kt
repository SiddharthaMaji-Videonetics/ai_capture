package com.videonatics.smartcapture.core.data.repository

import com.videonatics.smartcapture.core.data.datasource.local.CoreLocalDataSource
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import jakarta.inject.Inject

class CoreRepositoryImpl @Inject constructor(
    private val local: CoreLocalDataSource
) : CoreRepository{
    override fun getThemeMode(): ThemeMode = local.getThemeMode()
    override fun setThemeMode(themeMode: ThemeMode) = local.setThemeMode(themeMode)
    override fun setOfflineAnalysis(offlineAnalysis: Boolean) = local.setOfflineAnalysis(offlineAnalysis)
    override fun getOfflineAnalysis(): Boolean = local.getOfflineAnalysis()
    override fun setBoundBox(it: Boolean) = local.setBoundBox(it)
    override fun getBoundBoxOption(): Boolean = local.getBoundBoxOption()
    override fun isDepthDetectionOn(): Boolean = local.isDepthDetectionOn()
    override suspend fun setDepthDetection(isDepthEnabled: Boolean) = local.setDepthDetection(isDepthEnabled)
}