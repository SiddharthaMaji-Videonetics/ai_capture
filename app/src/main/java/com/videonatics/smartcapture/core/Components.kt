package com.videonatics.smartcapture.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.videonatics.smartcapture.capture.domain.model.Tilt

@Composable
fun rememberTiltState(): State<Tilt> {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tiltManager = remember {
        TiltSensorManager(context)
    }

    val tiltState = tiltManager.tilt.collectAsState()

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> tiltManager.start()
                Lifecycle.Event.ON_PAUSE -> tiltManager.stop()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tiltManager.stop()
        }
    }

    return tiltState
}

@Composable
fun TiltPopup() {

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                //CircularProgressIndicator(color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Too much left/right tilt!",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun InstallArCorePopup(){
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                //CircularProgressIndicator(color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ARCore not installed",
                    color = Color.White
                )

                Text(
                    text = "please install ARCore to continue",
                    color = Color.DarkGray
                )
            }
        }
    }
}