package com.videonatics.smartcapture.feature.ocr.presentation.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.ui.theme.LocalTextPrimaryColor
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrEditBottomSheet(sheetState: SheetState,
                       ocrString: String = "",
                       onEditedOcr: (String)-> Unit = {},
                       onDismiss: ()-> Unit, ) {
    val scope = rememberCoroutineScope()
    var mutableOcrString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        mutableOcrString = ocrString
    }

    return ModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                onDismiss.invoke()
            }

        },
        sheetState = sheetState,

        ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text("Edit OCR",
                style = TextStyle(
                    color = LocalTextPrimaryColor.current,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                ),
                modifier = Modifier.padding(bottom = 5.dp))
            HorizontalDivider()
            TextField(
                placeholder = {
                    Text("Selected OCR text.")
                },
                value = mutableOcrString,
                onValueChange = {
                    mutableOcrString = it
                },
                modifier = Modifier
                    .fillMaxWidth().padding(5.dp)
            )
            Spacer(modifier = Modifier.padding(5.dp))
            Button(onClick = {
                scope.launch {
                    sheetState.hide()
                    onEditedOcr.invoke(ocrString)
                }

            }) {
                Text("Next")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun OcrEditorBottomSheetPreview() {
    SmartCaptureTheme {
        OcrEditBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = {}
        )
    }
}