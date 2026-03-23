package com.videonatics.smartcapture.core.domain.repository

import com.videonatics.smartcapture.ui.theme.ThemeMode

interface CoreRepository {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(themeMode: ThemeMode)
    fun setOfflineAnalysis(offlineAnalysis: Boolean)
    fun getOfflineAnalysis(): Boolean
    fun setBoundBox(it: Boolean)
    fun getBoundBoxOption(): Boolean
    fun isDepthDetectionOn(): Boolean
    suspend fun setDepthDetection(isDepthEnabled: Boolean)
}