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
import android.graphics.Rect
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.byteArrayToBitmap
import com.videonatics.smartcapture.core.domain.model.OcrBox
import com.videonatics.smartcapture.core.presentation.view.components.PixelPerfectOCRViewer
import com.videonatics.smartcapture.core.presentation.view.components.ZoomableImageWithSelectableOCR
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.feature.ocr.presentation.viewmodel.OcrViewModel
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun OcrSelectorPage(
    navController: NavController,
    sharedViewModel : SharedCaptureViewModel
) {
    //val imageBytes = sharedViewModel.capturedImage
    val viewModel = hiltViewModel<OcrViewModel>()
    val capturedImageArray = sharedViewModel.capturedImage
    var ocrList by remember { mutableStateOf<List<OcrBox>>(emptyList()) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(capturedImageArray) {
        capturedImageArray?.apply {
            capturedImage = byteArrayToBitmap(capturedImageArray)
           viewModel.getOcrFromInputImage(byteArrayToBitmap(capturedImageArray)){
                ocrList = it
            }
        }

    }
    return OcrSelectorPageComponent(capturedImage, ocrList){
        navController.navigate(PageRoute.CAPTURE.name) {
            popUpTo(PageRoute.IMAGE_DETAILS.name) { inclusive = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSelectorPageComponent(img: Bitmap? = null,
                             ocrBoxes: List<OcrBox> = emptyList(),
                             onBack: () -> Unit) {

    var selectedOcrItmCount by remember { mutableStateOf(0) }

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

        },
        bottomBar = {
            BottomAppBar() {
                Box (
                    modifier = Modifier.fillMaxWidth()

                ){
                    Text("Selected Items: ${selectedOcrItmCount}/${ocrBoxes.size}",
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                }
            }
        },
        floatingActionButton = {
            AnimatedContent(
                targetState = selectedOcrItmCount>0,
                label = "Edit OCR"
            ) {isEditOcr ->
                if (isEditOcr) {
                    ExtendedFloatingActionButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit OCR"
                            )
                        },
                        text = {
                            Text("Edit OCR")
                        },
                        onClick = {

                        }
                    )
                }else{
                    ExtendedFloatingActionButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.NavigateNext,
                                contentDescription = "Next"
                            )
                        },
                        text = {
                            Text("Next")
                        },
                        onClick = {

                        }
                    )
                }
            }

        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
        ){
            AnimatedVisibility(visible = img != null) {
                img?.let {
                    PixelPerfectOCRViewer(
                        bitmap = it.asImageBitmap(),
                        ocrBoxes = ocrBoxes
                    ){selectedOcrIds ->
                        selectedOcrItmCount = selectedOcrIds.distinct().size
                    }
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