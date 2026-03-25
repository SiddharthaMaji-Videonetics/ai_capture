package com.videonatics.smartcapture.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.root.PageRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    fun loadSplash(onSplashLoaded: (PageRoute) -> Unit){
        viewModelScope.launch {
            delay(1000)
            onSplashLoaded.invoke(PageRoute.HOME)


        }
    }
}