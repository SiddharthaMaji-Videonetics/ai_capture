package com.videonatics.smartcapture.core.data.datasource.local

import com.videonatics.smartcapture.core.PrefDelegate
import com.videonatics.smartcapture.ui.theme.ThemeMode
import jakarta.inject.Inject

interface CoreLocalDataSource {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(themeMode: ThemeMode)
    fun setOfflineAnalysis(offlineAnalysis: Boolean)
    fun getOfflineAnalysis(): Boolean
    fun setBoundBox(isBoundBox: Boolean)
    fun getBoundBoxOption(): Boolean
    fun isDepthDetectionOn(): Boolean
    suspend fun setDepthDetection(depthEnabled: Boolean)
}

class CoreLocalDataSourceImpl @Inject constructor(private val prefDelegate: PrefDelegate) : CoreLocalDataSource{
    override fun getThemeMode(): ThemeMode {
        return when (prefDelegate.themeModePref) { // Change 'while' to 'when'
            ThemeMode.DARK.toString() -> ThemeMode.DARK
            ThemeMode.LIGHT.toString() -> ThemeMode.LIGHT
            else -> ThemeMode.LIGHT
        }
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        prefDelegate.themeModePref = themeMode.toString()
    }

    override fun setOfflineAnalysis(offlineAnalysis: Boolean) {
        prefDelegate.offlineAnalysisPref = offlineAnalysis
    }

    override fun getOfflineAnalysis(): Boolean {
        return prefDelegate.offlineAnalysisPref
    }

    override fun setBoundBox(isBoundBox: Boolean) {
        prefDelegate.boundBoxPref = isBoundBox
    }

    override fun getBoundBoxOption(): Boolean {
        return prefDelegate.boundBoxPref
    }

    override fun isDepthDetectionOn(): Boolean {
        return prefDelegate.depthDetection
    }

    override suspend fun setDepthDetection(depthEnabled: Boolean) {
        prefDelegate.depthDetection = depthEnabled
    }

}