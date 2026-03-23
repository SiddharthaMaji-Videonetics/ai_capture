package com.videonatics.smartcapture.core.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedCoreViewModel @Inject constructor(
    private val coreRepository: CoreRepository
) : ViewModel() {
    var themeMode = coreRepository.getThemeMode()
}