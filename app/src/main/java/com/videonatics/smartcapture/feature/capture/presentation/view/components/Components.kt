package com.videonatics.smartcapture.feature.capture.presentation.view.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.feature.capture.presentation.view.CameraCapturePageComponent
import com.videonatics.smartcapture.feature.capture.presentation.view.FlashController
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepthScanningBottomSheet(sheetState: SheetState,
                      onDismiss: ()-> Unit, ) {
    val scope = rememberCoroutineScope()
    return ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier
                .fillMaxWidth()

        ) {

            MotionSlider(
                value = 0.5f,
                onValueChange = {},
                onDirection = {},
                seconds = 5,
                isScanning = true,
                onScanDepth = {
//                    scope.launch {
//                        sheetState.hide()
//                        delay(100)
//
//                    }
                }
            )

        }
    }
}

@Composable
fun MotionSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    onDirection: (Float) -> Unit,
    isScanning: Boolean,
    onScanDepth: () -> Unit,
    seconds: Int
) {
    Column(
//        modifier = modifier
//            .background(
//                Color(0xFF2C2C2C).copy(alpha = 0.9f),
//                RoundedCornerShape(16.dp)
//            )
//            .padding(16.dp)
    ) {

        // Top Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {

            // LEFT → emits -ve
            IconButton(onClick = {
                onDirection(-1f)
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Left",
                    tint = Color.White
                )
            }

            Text(
                text = "Move Phone left and right",
                color = Color.White
            )

            // RIGHT → emits +ve
            IconButton(onClick = {
                onDirection(+1f)
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Right",
                    tint = Color.White
                )
            }
        }


        // Slider (manual control)
        Slider(
            value = value,
            onValueChange = {
                onValueChange(it)

                // Optional: convert slider movement → direction
                // center = 0.5f
                val direction = when {
                    it > 0.5f -> +1f
                    it < 0.5f -> -1f
                    else -> 0f
                }
                onDirection(direction)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFB74D),
                activeTrackColor = Color(0xFF494357)
            )
        )

        Spacer(modifier = Modifier.height(5.dp))

        AnimatedVisibility(visible = isScanning,
            enter = scaleIn(
                initialScale = 0.6f
            ) + fadeIn(),

            exit = scaleOut(
                targetScale = 0.6f
            ) + fadeOut(
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)) {
            // Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color.LightGray
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "$seconds Sec.",
                    color = Color(0xFFFFB74D)
                )
            }
        }

        AnimatedVisibility(visible = !isScanning,
            enter = scaleIn(
                initialScale = 0.6f
            ) + fadeIn(),

            exit = scaleOut(
                targetScale = 0.6f
            ) + fadeOut(
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
            Button(onClick = onScanDepth) {
                Text("Scan")
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun CDepthScanningBottomSheetPreview() {
    SmartCaptureTheme {
        DepthScanningBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = {}
        )
    }
}