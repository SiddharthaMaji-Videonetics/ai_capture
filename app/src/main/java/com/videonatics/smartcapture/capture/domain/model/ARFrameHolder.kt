package com.videonatics.smartcapture.capture.domain.model

import com.google.ar.core.Frame

object ARFrameHolder {
    @Volatile
    var latestFrame: Frame? = null
}