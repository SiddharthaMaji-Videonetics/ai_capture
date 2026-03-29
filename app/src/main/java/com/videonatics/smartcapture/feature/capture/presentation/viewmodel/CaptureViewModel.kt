package com.videonatics.smartcapture.feature.capture.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.capture.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repo: CaptureRepository
) : ViewModel(){
    private val _location = MutableStateFlow<Pair<Double, Double>?>(null)
    val location: StateFlow<Pair<Double, Double>?> = _location


    fun startLocationUpdates() {
        viewModelScope.launch {
            repo.getLocationFlow().collect {
                _location.value = it
            }
        }
    }
}