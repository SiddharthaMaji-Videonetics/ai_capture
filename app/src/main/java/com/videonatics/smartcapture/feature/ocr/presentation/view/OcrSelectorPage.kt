package com.videonatics.smartcapture.feature.ocr.presentation.view

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.domain.model.OcrBox
import com.videonatics.smartcapture.core.presentation.view.components.ZoomableImageWithSelectableOCR
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.feature.capture.presentation.view.CameraCapturePageComponent
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun OcrSelectorPage(
    navController: NavController,
    sharedViewModel : SharedCaptureViewModel
) {
    //val imageBytes = sharedViewModel.capturedImage
    val fullCapturedImage = sharedViewModel.capturedFullImage
    return OcrSelectorPageComponent(fullCapturedImage){
        navController.navigate(PageRoute.CAPTURE.name) {
            popUpTo(PageRoute.IMAGE_DETAILS.name) { inclusive = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSelectorPageComponent(img: Bitmap? = null, onBack: () -> Unit) {
    val sampleOCR = listOf(
        OcrBox(
            id = 1,
            text = "Hello World",
            rect = Rect(100f, 200f, 500f, 300f)
        ),
        OcrBox(
            id = 2,
            text = "Jetpack Compose",
            rect = Rect(120f, 350f, 700f, 450f)
        ),
        OcrBox(
            id = 3,
            text = "OCR Detection",
            rect = Rect(200f, 550f, 800f, 650f)
        ),
        OcrBox(
            id = 4,
            text = "Bounding Box",
            rect = Rect(150f, 750f, 650f, 850f)
        ),
        OcrBox(
            id = 5,
            text = "Zoom + Pan Test",
            rect = Rect(100f, 950f, 900f, 1050f)
        ),
        OcrBox(
            id = 6,
            text = "Small Text",
            rect = Rect(50f, 1100f, 300f, 1150f)
        ),
        OcrBox(
            id = 7,
            text = "Bottom Area",
            rect = Rect(200f, 1500f, 800f, 1600f)
        )
    )
    return Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Ocr Selector")
                },

                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBack.invoke()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }

                },
            )
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
        ){
            AnimatedVisibility(visible = img != null) {
                img?.let {
                    ZoomableImageWithSelectableOCR(
                        bitmap = it.asImageBitmap(),
                        ocrBoxes = sampleOCR
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CapturePagePreview() {
    SmartCaptureTheme {
        OcrSelectorPageComponent {

        }
    }
}