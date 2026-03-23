package com.videonatics.smartcapture.core

import android.content.Context
import androidx.core.content.edit
import com.videonatics.smartcapture.ui.theme.ThemeMode

class PrefDelegate(context: Context) {

    companion object {
        private const val THEME_MODE = "theme_mode"
        private const val OFFLINE_ANALYSIS = "offline_analysis"

        private const val BOUND_BOX = "bound_box"

        private const val DEPTH_DETECTION = "depth_detection"
    }

    private val sharedPreferences = context.getSharedPreferences(
        "${context.packageName}.preferences", Context.MODE_PRIVATE
    )



    var themeModePref: String?
        get() = sharedPreferences.getString(THEME_MODE, ThemeMode.LIGHT.toString())
        set(value) = sharedPreferences.edit { putString(THEME_MODE, value) }

    var offlineAnalysisPref: Boolean
        get() = sharedPreferences.getBoolean(OFFLINE_ANALYSIS, true)
        set(value) = sharedPreferences.edit { putBoolean(OFFLINE_ANALYSIS, value) }

    var boundBoxPref: Boolean
        get() = sharedPreferences.getBoolean(BOUND_BOX, true)
        set(value) = sharedPreferences.edit { putBoolean(BOUND_BOX, value) }

    var depthDetection: Boolean
        get() = sharedPreferences.getBoolean(DEPTH_DETECTION, false)
        set(value) = sharedPreferences.edit { putBoolean(DEPTH_DETECTION, value) }

}