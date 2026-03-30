package com.videonatics.smartcapture.feature.imagedetails.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.bitmapToMultipart
import com.videonatics.smartcapture.core.bitmapToTensor
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.core.parseYOLO
import com.videonatics.smartcapture.feature.imagedetails.domain.repository.ImageDetailsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageDetailsViewModel @Inject constructor(
    application: Application,
    private val repository: ImageDetailsRepository,
    private val coreRepository: CoreRepository
) : AndroidViewModel(application) {
    //private val detector = Yolov8nFloat32.newInstance(application)

//    fun detect(bitmap: Bitmap): List<Detection> {
//
//        val outputs = detector.process(bitmapToTensor(bitmap))
//
//        val result = outputs.outputFeature0AsTensorBuffer
//
//        val detections = result.floatArray
//
//        return parseYOLO(
//            detections,
//            bitmap.width,
//            bitmap.height
//        )
//    }

    init {

    }

    fun getImageAnalysisData(data: Bitmap, onTimeOut:(String)-> Unit, onDetails: (List<Pair<String, Any>>, ocr: String) -> Unit){
        viewModelScope.launch {
            when(val response = repository.getImageAnalysis(bitmapToMultipart(data))){
                is com.videonatics.smartcapture.core.network.Result.Success -> {

                    onDetails(response.data.tags, response.data.ocrText)
                }
                is com.videonatics.smartcapture.core.network.Result.Error -> {
                    if(response.exception.message !=null){
                            onTimeOut.invoke(response.exception.message?:"Timeout")
                        //onDetails(emptyList(),response.exception.message!!)
                    }
                    //onDetails(emptyList(),"Unable to extract OCR data")
                }
            }
        }
    }

    override fun onCleared() {
       // detector.close()
        super.onCleared()
    }

    fun isOfflineAnalysis() : Boolean{
        return coreRepository.getOfflineAnalysis()
    }

//    val labels = listOf(
//        "person",
//        "bicycle",
//        "car",
//        "motorcycle",
//        "airplane",
//        "bus",
//        "train",
//        "truck",
//        "boat",
//        "traffic light",
//        "fire hydrant",
//        "stop sign",
//        "parking meter",
//        "bench",
//        "bird",
//        "cat",
//        "dog",
//        "horse",
//        "sheep",
//        "cow",
//        "elephant",
//        "bear",
//        "zebra",
//        "giraffe",
//        "backpack",
//        "umbrella",
//        "handbag",
//        "tie",
//        "suitcase",
//        "frisbee",
//        "skis",
//        "snowboard",
//        "sports ball",
//        "kite",
//        "baseball bat",
//        "baseball glove",
//        "skateboard",
//        "surfboard",
//        "tennis racket",
//        "bottle",
//        "wine glass",
//        "cup",
//        "fork",
//        "knife",
//        "spoon",
//        "bowl",
//        "banana",
//        "apple",
//        "sandwich",
//        "orange",
//        "broccoli",
//        "carrot",
//        "hot dog",
//        "pizza",
//        "donut",
//        "cake",
//        "chair",
//        "couch",
//        "potted plant",
//        "bed",
//        "dining table",
//        "toilet",
//        "tv",
//        "laptop",
//        "mouse",
//        "remote",
//        "keyboard",
//        "cell phone",
//        "microwave",
//        "oven",
//        "toaster",
//        "sink",
//        "refrigerator",
//        "book",
//        "clock",
//        "vase",
//        "scissors",
//        "teddy bear",
//        "hair drier",
//        "toothbrush"
//    )
}