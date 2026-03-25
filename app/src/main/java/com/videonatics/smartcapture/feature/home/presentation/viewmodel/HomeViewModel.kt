package com.videonatics.smartcapture.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.feature.home.domain.model.CaptureItem

class HomeViewModel : ViewModel() {
    private val _capturedData = ArrayList<CaptureItem>()
    val capturedData: List<CaptureItem> = _capturedData

    init {
        _capturedData.addAll(
            listOf(
//                CaptureItem(
//                    id = 1,
//                    name = "Capture 1",
//                    desc = "Description 1",
//                    timestamp = System.currentTimeMillis(),
//                    img = "https://example.com/image1.jpg"
//                ),
//                CaptureItem(
//                    id = 2,
//                    name = "Capture 2",
//                    desc = "Description 2",
//                    timestamp = System.currentTimeMillis(),
//                    img = "https://example.com/image2.jpg"
//                )
            )
        )
    }
}