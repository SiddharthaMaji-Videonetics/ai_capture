package com.videonatics.smartcapture.capture.presentation.view.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.videonatics.smartcapture.capture.presentation.view.FlashController

@Composable
fun FlashToggle() {
    val context = LocalContext.current
    val flashController = remember { FlashController(context) }

    var isOn by remember { mutableStateOf(false) }

    Button(onClick = {
        isOn = !isOn
        flashController.setFlash(isOn)
    }) {
        Text(if (isOn) "Flash ON 🔦" else "Flash OFF")
    }
}