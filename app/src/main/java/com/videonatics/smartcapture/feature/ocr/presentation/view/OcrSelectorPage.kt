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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.byteArrayToBitmap
import com.videonatics.smartcapture.core.domain.model.OcrBox
import com.videonatics.smartcapture.core.presentation.view.components.PixelPerfectOCRViewer
import com.videonatics.smartcapture.core.presentation.view.components.ZoomableImageWithSelectableOCR
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.feature.ocr.presentation.view.components.OcrEditBottomSheet
import com.videonatics.smartcapture.feature.ocr.presentation.viewmodel.OcrViewModel
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@OptIn(ExperimentalMaterial3Api::class)
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
    var isOcrLoaded by remember { mutableStateOf(false) }


    BackHandler(true) {
        navController.navigate(PageRoute.CAPTURE.name) {
            popUpTo(PageRoute.OCR_SELECT.name) { inclusive = true }
        }
    }

    LaunchedEffect(capturedImageArray) {
        capturedImageArray?.apply {
            capturedImage = byteArrayToBitmap(capturedImageArray)
           viewModel.getOcrFromInputImage(byteArrayToBitmap(capturedImageArray)){
                ocrList = it
               isOcrLoaded = true
            }
        }

    }
    return OcrSelectorPageComponent(capturedImage, ocrList,isOcrLoaded, onNextPage = {
        sharedViewModel.ocrString = it
        navController.navigate(PageRoute.IMAGE_DETAILS.name){}
    } ){
        navController.navigate(PageRoute.CAPTURE.name) {
            popUpTo(PageRoute.OCR_SELECT.name) { inclusive = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSelectorPageComponent(img: Bitmap? = null,
                             ocrBoxes: List<OcrBox> = emptyList(),
                             isOcrLoaded: Boolean = false,
                             onNextPage:(ocr: String)-> Unit = {},
                             onBack: () -> Unit) {


    var selectedOcrItmCount by remember { mutableIntStateOf(0) }
    var editOcrBottomSheet by remember { mutableStateOf(false) }

    val ocrEditBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var ocrFullString = ""

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
            AnimatedVisibility(visible = ocrBoxes.isNotEmpty()) {
                BottomAppBar() {
                    Box (
                        modifier = Modifier.fillMaxWidth()

                    ){
                        Text("Selected Items: ${selectedOcrItmCount}/${ocrBoxes.size}",
                            modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    }
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
                            editOcrBottomSheet = true
                        }
                    )
                }else{
                    AnimatedVisibility(
                        visible = ocrBoxes.isEmpty()
                    ) {
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
                                onNextPage.invoke(ocrFullString)
                            }
                        ) }

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

                        val selectedIdSet = selectedOcrIds.toHashSet()

                        selectedOcrItmCount = selectedIdSet.size

                        val selectedOcrBoxes = ocrBoxes.filter {box -> box.id in selectedIdSet }

                        ocrFullString = buildString {
                            selectedOcrBoxes.forEachIndexed { index, box ->
                                if (index > 0) append(" ")
                                append(box.text)
                            }
                        }
                    }
                }
            }
            if(editOcrBottomSheet) {
                OcrEditBottomSheet(ocrEditBottomSheetState,
                    ocrFullString,
                    onEditedOcr = {ocr ->
                        editOcrBottomSheet = false
                        onNextPage.invoke(ocr)

                }){
                    editOcrBottomSheet = false
                }
            }
            AnimatedVisibility(visible = !isOcrLoaded) {
                BasicAlertDialog(onDismissRequest = {

                }) {
                    Card(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                "Loading OCR...",
                                modifier = Modifier.padding(8.dp)
                            )
                        }
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