package com.videonatics.smartcapture.feature.imagedetails.presentation.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.videonatics.smartcapture.core.getAltitude
import com.videonatics.smartcapture.core.presentation.view.components.ZoomableImage
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.core.toConfidenceColor
import com.videonatics.smartcapture.core.toPercentageString
import com.videonatics.smartcapture.feature.imagedetails.presentation.viewmodel.ImageDetailsViewModel
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.LocalCameraBackground
import com.videonatics.smartcapture.ui.theme.LocalPrimarySurfaceBackground
import com.videonatics.smartcapture.ui.theme.LocalTextPrimaryColor
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import okio.ByteString.Companion.toByteString

@Composable
fun ImageDetailsPage(navController: NavController,
                     sharedViewModel : SharedCaptureViewModel) {

    if (LocalInspectionMode.current) {
        ImageDetailsPageComponent()
        return
    }

    val context = LocalContext.current
    val viewModel: ImageDetailsViewModel = hiltViewModel()
    var overallObject by remember { mutableStateOf<List<Pair<String, Any>>>(emptyList()) }
    var imgLbl by remember { mutableStateOf<List<Pair<String, Any>>>(emptyList()) }
    var ocrLbl by remember { mutableStateOf("---") }
    var isLoading by remember { mutableStateOf(false) }
    var altitude by remember { mutableStateOf(0.0) }



    val imageBytes = sharedViewModel.capturedImage
    val fullCapturedImage = sharedViewModel.capturedFullImage

    Log.e("imageBytes", "imageBytes: ${imageBytes?.toByteString()}")

    val img = remember(imageBytes) {
        imageBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }

    fun getLocalAnalysis(bitmap: ImageBitmap) {
        val inputImage = InputImage.fromBitmap(bitmap.asAndroidBitmap(), 0)


    }

    LaunchedEffect(Unit) {
        getAltitude(context){
            it?.apply {
                altitude = it
            }

        }
    }

    BackHandler(true) {
        navController.navigate(PageRoute.OCR_SELECT.name) {
            popUpTo(PageRoute.IMAGE_DETAILS.name) { inclusive = true }
        }
    }

    LaunchedEffect(img) {
        ocrLbl = sharedViewModel.ocrString
        isLoading = true
        img?.let {
                getLocalAnalysis(it)
                isLoading = false

        }
    }




    ImageDetailsPageComponent(img = img,
        fullCapturedImage = fullCapturedImage,
        location = sharedViewModel.capturedLocation,
        altitude,
        ocrLbl = ocrLbl,
        navController,
        isLoading = isLoading)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsPageComponent(
    img: ImageBitmap? = null,
    fullCapturedImage: Bitmap? = null,
    location: Pair<Double, Double>? = null,
    altitude: Double = 0.0,
    ocrLbl: String = "N/A",
    navController: NavController? = null,
    isLoading: Boolean = false,
    isTimeOut: Boolean = false
) {

    var isMoreDetails by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Image Details")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController?.navigate(PageRoute.OCR_SELECT.name) {
                                popUpTo(PageRoute.IMAGE_DETAILS.name) { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }

                },
                actions = {
//                    Switch(isOnlineAnalysis,
//                        onCheckedChange = {
//                        isOfflineAnalysisOn(it)
//                    })
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(LocalCameraBackground.current),
                        contentAlignment = Alignment.Center
                    ) {
                        this@Column.AnimatedVisibility(visible = img != null) {
                            fullCapturedImage?.let {
                                ZoomableImage(
                                    bitmap = it.asImageBitmap(),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxSize()
                            .background(LocalPrimarySurfaceBackground.current),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (isLoading){
                            Card(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .align(Alignment.Center),

                            ) { Column(
                                modifier = Modifier.padding(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {

                                CircularProgressIndicator()
                                Text("Loading data...", color = LocalTextPrimaryColor.current)
                            }}


                    }else if(isTimeOut){
                            Card(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .align(Alignment.Center),

                                ) { Column(
                                modifier = Modifier.padding(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {

                                Text("Loading time out", color = LocalTextPrimaryColor.current)
                            }}
                    }else{
                        Column() {
                            Text("Analytical Data", modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp, vertical = 10.dp),
                                fontSize = 18.sp,
                                color = LocalTextPrimaryColor.current,
                                fontWeight = FontWeight.Bold)

                            LazyColumn( modifier = Modifier
                                .fillMaxSize()) {

                                item {
                                    HorizontalDivider(
                                        color = Color.Black,
                                    )
                                }
                                item {
                                    Card( modifier = Modifier.padding(10.dp),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = 5.dp
                                        ),) {
                                        ListItem(
                                            headlineContent ={
                                                Column() {
                                                    Text("OCR", modifier = Modifier
                                                        .padding(horizontal = 5.dp, vertical = 5.dp),
                                                        fontSize = 14.sp,
                                                        color = LocalTextPrimaryColor.current,
                                                        fontWeight = FontWeight.Medium)
                                                    HorizontalDivider(
                                                        color = LocalTextPrimaryColor.current,
                                                    )
                                                }

                                            },
                                            supportingContent = {
                                                Text(if(ocrLbl.trim().isEmpty())
                                                    "Nothing Found"
                                                else ocrLbl,
                                                    modifier = Modifier.padding(5.dp))
                                            }
                                        )
                                    }

                                }
                                item {
                                    Card( modifier = Modifier.padding(10.dp),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = 5.dp
                                        ),) {
                                        ListItem(
                                            headlineContent ={
                                                Column() {
                                                    Text("Capture Info", modifier = Modifier
                                                        .padding(horizontal = 5.dp, vertical = 5.dp),
                                                        fontSize = 14.sp,
                                                        color = LocalTextPrimaryColor.current,
                                                        fontWeight = FontWeight.Medium)
                                                    HorizontalDivider(
                                                        color = LocalTextPrimaryColor.current,
                                                    )
                                                }

                                            },
                                            supportingContent = {
                                                Column( modifier = Modifier
                                                    .padding(5.dp)) {
                                                    ListItem(

                                                            headlineContent = {
                                                                Text("Location", modifier = Modifier
                                                                    .padding(horizontal = 5.dp, vertical = 5.dp),
                                                                    fontSize = 18.sp,
                                                                    color = LocalTextPrimaryColor.current,
                                                                    fontWeight = FontWeight.W200)
                                                            },
                                                            trailingContent = {
                                                                if(location !=null){
                                                                    Text(
                                                                        text = "${location.first}° N,${location.second}° E",
                                                                        maxLines = 20,
                                                                        fontSize = 14.sp,
                                                                        color = LocalTextPrimaryColor.current,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }else{
                                                                    Text(
                                                                        text = "Unknown",
                                                                        maxLines = 20,
                                                                        fontSize = 14.sp,
                                                                        color = LocalTextPrimaryColor.current,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }

                                                            })
                                                    ListItem(

                                                        headlineContent = {
                                                            Text("Altitude", modifier = Modifier
                                                                .padding(horizontal = 5.dp, vertical = 5.dp),
                                                                fontSize = 18.sp,
                                                                color = LocalTextPrimaryColor.current,
                                                                fontWeight = FontWeight.W200)
                                                        },
                                                        trailingContent = {
                                                            Text(
                                                                text = "${altitude} m",
                                                                maxLines = 20,
                                                                fontSize = 14.sp,
                                                                color = LocalTextPrimaryColor.current,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        })


                                                }
                                            }
                                        )
                                    }

                                }
                                item {
                                    HorizontalDivider(
                                        color = Color.Black,
                                    )
                                }
                                item {
                                    Box( modifier = Modifier
                                        .fillMaxSize()
                                        ){
                                        if(isMoreDetails) {
                                            Column {

                                                Card(
                                                    modifier = Modifier.padding(10.dp),
                                                    elevation = CardDefaults.cardElevation(
                                                        defaultElevation = 5.dp
                                                    ),
                                                ) {
                                                    ListItem(
                                                        headlineContent ={
                                                            Column() {
                                                                Text("Equipment Details", modifier = Modifier
                                                                    .padding(horizontal = 5.dp, vertical = 5.dp),
                                                                    fontSize = 14.sp,
                                                                    color = LocalTextPrimaryColor.current,
                                                                    fontWeight = FontWeight.Medium)
                                                                HorizontalDivider(
                                                                    color = LocalTextPrimaryColor.current,
                                                                )
                                                            }

                                                        },
                                                        supportingContent = {
//                                                            if(imgLbl.isEmpty()){
//                                                                Text("Details not specified", modifier = Modifier.padding(5.dp))
//                                                            }else{
//                                                            Column( modifier = Modifier
//                                                                .padding(5.dp)) {
//                                                                imgLbl.forEach {obj ->
//                                                                    ListItem(
//
//                                                                        headlineContent = {
//                                                                            Text(obj.first, modifier = Modifier
//                                                                                .padding(horizontal = 5.dp, vertical = 5.dp),
//                                                                                fontSize = 18.sp,
//                                                                                color = LocalTextPrimaryColor.current,
//                                                                                fontWeight = FontWeight.W200)
//                                                                        },
//                                                                        trailingContent = {
//                                                                            Text(
//                                                                                text = if(obj.second is Float) (obj.second as Float).toPercentageString() else obj.second.toString(),
//                                                                                maxLines = 20,
//                                                                                fontSize = 14.sp,
//                                                                                color = if(obj.second is Float) (obj.second as Float).toConfidenceColor()
//                                                                                else
//                                                                                    if(obj.second.toString().contains("Good")) 1F.toConfidenceColor()
//                                                                                    else if(obj.second.toString().contains("Bad")) 0F.toConfidenceColor()
//                                                                                    else LocalTextPrimaryColor.current,
//                                                                                fontWeight = FontWeight.Bold
//                                                                            )
//                                                                        })
//
//
//                                                                }
//                                                            }
//                                                        }
                                                        }
                                                    )
                                                }

//                                                TextButton(onClick = {
//                                                    isMoreDetails = false
//                                                }) {
//                                                    Text("Show Less.",
//                                                        color = Color.Blue,
//                                                    )
//                                                }
                                                Card(
                                                    modifier = Modifier.padding(10.dp),
                                                    elevation = CardDefaults.cardElevation(
                                                        defaultElevation = 5.dp
                                                    ),
                                                ) {
                                                    ListItem(
                                                        headlineContent ={
                                                            Column() {
                                                                Text("Summary", modifier = Modifier
                                                                    .padding(horizontal = 5.dp, vertical = 5.dp),
                                                                    fontSize = 14.sp,
                                                                    color = LocalTextPrimaryColor.current,
                                                                    fontWeight = FontWeight.Medium)
                                                                HorizontalDivider(
                                                                    color = LocalTextPrimaryColor.current,
                                                                )
                                                            }

                                                        },
                                                        supportingContent = {
//                                                            if(overallObject.isEmpty()){
//                                                                Text("Unidentified", modifier = Modifier.padding(5.dp))
//                                                            }else{
//                                                                Column( modifier = Modifier
//                                                                    .padding(5.dp)) {
//                                                                    overallObject.forEach {obj ->
//                                                                        ListItem(
//
//                                                                            headlineContent = {
//                                                                                Text(
//                                                                                    text = obj.second.toString(),
//                                                                                    fontSize = 14.sp,
//                                                                                    color = LocalTextPrimaryColor.current,
//                                                                                    fontWeight = FontWeight.Bold
//                                                                                )
//                                                                            })
//
//
//                                                                    }
//                                                                }
//                                                            }

                                                        }
                                                    )
                                                }
                                            }
                                        }else{
                                            TextButton(onClick = {
                                                isMoreDetails = true
                                            },
                                                modifier = Modifier
                                                    .align(Alignment.Center)) {
                                                Text("Show more.",
                                                    color = Color.Blue,
                                                )
                                            }
                                        }
                                    }

                                }






                            }

                        }

        }


                    }
                }





    }
}

@Preview(showBackground = true)
@Composable
fun ImageDetailsPreview() {
    SmartCaptureTheme {
        ImageDetailsPageComponent()
    }
}