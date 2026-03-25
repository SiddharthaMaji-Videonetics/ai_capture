package com.videonatics.smartcapture.feature.imagedetails.data.mapper

import com.videonatics.smartcapture.core.data.model.ImageDetailsResponse
import com.videonatics.smartcapture.core.data.model.Tag
import com.videonatics.smartcapture.feature.imagedetails.domain.model.ImageDetails

fun ImageDetailsResponse.toDomain(): ImageDetails {
    val pairs = ArrayList<Pair<String, Any>>()
    this.tags.forEach {
        if(it.type != null){
            val confidence = it.confidence?.toFloat() ?: 0f
            pairs.add(it.type to confidence)
        }
        if(it.summary !=null){

            pairs.add("Summary" to it.summary)

        }
        if(it.evaluation !=null){
            pairs.add("Quality" to it.evaluation.label)
        }

    }

    val ocrTxt =if(this.ocr.isNotEmpty()){
        this.ocr.filter { it.confidence> 0.5f }.sortedByDescending { it.confidence }.map { it.text }.toString().replace("[","").replace("]","")
    }else{
        ""
    }

    
    return ImageDetails(
        tags = pairs.toList(),
        ocrText = ocrTxt

    )
}
