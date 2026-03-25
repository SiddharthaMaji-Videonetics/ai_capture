package com.videonatics.smartcapture.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val coreRepository: CoreRepository
) : ViewModel(){
    fun isDarkTheme(): Boolean {
        return coreRepository.getThemeMode() == ThemeMode.DARK
    }

    fun setDerkTheme(isDarkTheme: Boolean) {
        coreRepository.setThemeMode(if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    fun setBoundBox(it: Boolean) {
        coreRepository.setBoundBox(it)

    }

    fun isBoundBoxEnabled(): Boolean {
        return coreRepository.getBoundBoxOption()
    }

    fun isDepthDetectionOn(): Boolean{
        return coreRepository.isDepthDetectionOn()
    }

    fun setDepthDetection(it: Boolean) {
        viewModelScope.launch {
            coreRepository.setDepthDetection(it)
        }
    }
}