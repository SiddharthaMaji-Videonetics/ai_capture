package com.videonatics.smartcapture.feature.capture.presentation.view.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.videonatics.smartcapture.feature.capture.presentation.view.FlashController

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

@Composable
fun LocationPermission(onGranted: () -> Unit) {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted()
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}