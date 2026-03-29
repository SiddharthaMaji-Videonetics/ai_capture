package com.videonatics.smartcapture.feature.ocr.presentation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OcrSelectorPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onResult: (String) -> Unit
) {
    return OcrSelectorPageComponent()
}

@Composable
fun OcrSelectorPageComponent() {
    return Scaffold() {
        Box(
            modifier = Modifier
                .padding(it)
        ){

        }
    }
}