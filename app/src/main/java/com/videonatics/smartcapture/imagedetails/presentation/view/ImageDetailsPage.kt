package com.videonatics.smartcapture.imagedetails.presentation.view

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
import com.videonatics.smartcapture.core.presentation.view.components.ZoomableImage
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.core.toConfidenceColor
import com.videonatics.smartcapture.core.toPercentageString
import com.videonatics.smartcapture.imagedetails.presentation.viewmodel.ImageDetailsViewModel
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
    val viewModel: ImageDetailsViewModel = hiltViewModel()
    var overallObject by remember { mutableStateOf<List<Pair<String, Any>>>(emptyList()) }
    var imgLbl by remember { mutableStateOf<List<Pair<String, Any>>>(emptyList()) }
    var ocrLbl by remember { mutableStateOf("---") }
    var isTimeOut by remember { mutableStateOf(false) }
    var isOnlineAnalysis = viewModel.isOfflineAnalysis()
    var isLoading by remember { mutableStateOf(false) }




    val imageBytes = sharedViewModel.capturedImage
    val fullCapturedImage = sharedViewModel.capturedFullImage

    Log.e("imageBytes", "imageBytes: ${imageBytes?.toByteString()}")

    val img = remember(imageBytes) {
        imageBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }

    fun getLocalOcr(bitmap: ImageBitmap, onOcr:(String) -> Unit){
        val inputImage = InputImage.fromBitmap(bitmap.asAndroidBitmap(), 0)

        // OCR
        val textRecognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )


        val textTask = textRecognizer.process(inputImage)

        Tasks.whenAllSuccess<List<Any>>(textTask)
            .addOnSuccessListener { results ->


                val text = results[0] as Text


                // OCR Text
                val ocrText = text.text

                // Merge everything (unique values)

                Log.d("OCR", ocrText)
                onOcr.invoke(ocrText)
            }
            .addOnCompleteListener {
                //imageProxy.close()
            }


        //            detector.process(inputImage)
        //                .addOnSuccessListener { objects ->
        //                    val allTags = objects.flatMap { obj ->
        //                        obj.labels.map { label ->
        //                            "${label.text} (${label.confidence})"
        //                        }
        //                    }
        //                    Log.d("imgTags", allTags.toString())
        //                }
        //                .addOnFailureListener { e ->
        //                    Log.e("imgTags", "Detection failed", e)
        //                }
    }
    fun getLocalAnalysis(bitmap: ImageBitmap) {
        val inputImage = InputImage.fromBitmap(bitmap.asAndroidBitmap(), 0)

        // Object Detector
        val objectOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

        val objectDetector = ObjectDetection.getClient(objectOptions)

        // Image Labeler
        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.5f)
                .build()
        )

        // OCR
        val textRecognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )


        val objectTask = objectDetector.process(inputImage)
        val labelTask = labeler.process(inputImage)
        val textTask = textRecognizer.process(inputImage)

        Tasks.whenAllSuccess<List<Any>>(objectTask, labelTask, textTask)
            .addOnSuccessListener { results ->

                val objects = results[0] as List<DetectedObject>
                val labels = results[1] as List<ImageLabel>
                val text = results[2] as Text

                // Object Tags
                val objectTags = objects.flatMap { obj ->
                    obj.labels
                        .filter { it.confidence > 0.5f }
                        .map { it.text to it.confidence }
                }

                // Image Labels (More detailed)
                val labelTags = labels
                    .filter { it.confidence > 0.5f }
                    .map { it.text to it.confidence }

                // OCR Text
                val ocrText = text.text

                // Merge everything (unique values)
                val finalTags = (objectTags + labelTags).toSet()

                Log.d("OBJECTS", objectTags.toString())

                overallObject = objectTags
                Log.d("LABELS", labelTags.toString())
                imgLbl = labelTags

                Log.d("OCR", ocrText)
                ocrLbl = ocrText

                Log.d("FINAL_TAGS", finalTags.toString())

            }
            .addOnCompleteListener {
                //imageProxy.close()
            }


        //            detector.process(inputImage)
        //                .addOnSuccessListener { objects ->
        //                    val allTags = objects.flatMap { obj ->
        //                        obj.labels.map { label ->
        //                            "${label.text} (${label.confidence})"
        //                        }
        //                    }
        //                    Log.d("imgTags", allTags.toString())
        //                }
        //                .addOnFailureListener { e ->
        //                    Log.e("imgTags", "Detection failed", e)
        //                }
    }

    BackHandler(true) {
        navController.navigate(PageRoute.CAPTURE.name) {
            popUpTo(PageRoute.IMAGE_DETAILS.name) { inclusive = true }
        }
    }

    LaunchedEffect(img) {

        isLoading = true
        img?.let {

//            val d = viewModel.detect(it.asAndroidBitmap())
//
//            d.forEach {
//                val label = viewModel.labels[it.classId]
//                Log.d("YOLO", "$label : ${it.score}")
//
//            }

            if(isOnlineAnalysis){

                val bmp = it.asAndroidBitmap()
            viewModel.getImageAnalysisData(bmp, onTimeOut = {
                isTimeOut = true
                isLoading = false
            }){tags,ocr->

//                getLocalOcr(it){ocr ->
//                    ocrLbl = ocr
//                }

                ocrLbl = ocr
                    imgLbl = tags.filter { it.first != "Summary" }
                overallObject = tags.filter { it.first == "Summary" }
                    isLoading = false
            }
            }else{
                getLocalAnalysis(it)
                isLoading = false
            }


        }
    }

    ImageDetailsPageComponent(img = img,
        fullCapturedImage = fullCapturedImage,
        overallObject = overallObject,
        imgLbl = imgLbl,
        ocrLbl = ocrLbl,
        navController,
        isLoading = isLoading,
        isOnlineAnalysis = isOnlineAnalysis,
        isOfflineAnalysisOn = {
            isOnlineAnalysis = it
            img?.apply {
                getLocalAnalysis(bitmap = this)
            }

        }, isTimeOut = isTimeOut)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsPageComponent(
    img: ImageBitmap? = null,
    fullCapturedImage: Bitmap? = null,
    overallObject: List<Pair<String, Any>> = emptyList(),
    imgLbl: List<Pair<String, Any>> = emptyList(),
    ocrLbl: String = "N/A",
    navController: NavController? = null,
    isLoading: Boolean = false,
    isOnlineAnalysis: Boolean = true,
    isOfflineAnalysisOn: (Boolean) -> Unit = {

    },
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
                            navController?.navigate(PageRoute.CAPTURE.name) {
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
                                                    if(overallObject.first().first.contains("Others")) "N/A"
                                                    else
                                                        "Nothing Found"
                                                else ocrLbl,
                                                    modifier = Modifier.padding(5.dp))
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
                                                            if(imgLbl.isEmpty()){
                                                                Text("Details not specified", modifier = Modifier.padding(5.dp))
                                                            }else{
                                                            Column( modifier = Modifier
                                                                .padding(5.dp)) {
                                                                imgLbl.forEach {obj ->
                                                                    ListItem(

                                                                        headlineContent = {
                                                                            Text(obj.first, modifier = Modifier
                                                                                .padding(horizontal = 5.dp, vertical = 5.dp),
                                                                                fontSize = 18.sp,
                                                                                color = LocalTextPrimaryColor.current,
                                                                                fontWeight = FontWeight.W200)
                                                                        },
                                                                        trailingContent = {
                                                                            Text(
                                                                                text = if(obj.second is Float) (obj.second as Float).toPercentageString() else obj.second.toString(),
                                                                                maxLines = 20,
                                                                                fontSize = 14.sp,
                                                                                color = if(obj.second is Float) (obj.second as Float).toConfidenceColor()
                                                                                else
                                                                                    if(obj.second.toString().contains("Good")) 1F.toConfidenceColor()
                                                                                    else if(obj.second.toString().contains("Bad")) 0F.toConfidenceColor()
                                                                                    else LocalTextPrimaryColor.current,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                        })


                                                                }
                                                            }
                                                        }
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
                                                            if(overallObject.isEmpty()){
                                                                Text("Unidentified", modifier = Modifier.padding(5.dp))
                                                            }else{
                                                                Column( modifier = Modifier
                                                                    .padding(5.dp)) {
                                                                    overallObject.forEach {obj ->
                                                                        ListItem(

                                                                            headlineContent = {
                                                                                Text(
                                                                                    text = obj.second.toString(),
                                                                                    fontSize = 14.sp,
                                                                                    color = LocalTextPrimaryColor.current,
                                                                                    fontWeight = FontWeight.Bold
                                                                                )
                                                                            })


                                                                    }
                                                                }
                                                            }

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