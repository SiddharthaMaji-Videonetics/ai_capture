package com.videonatics.smartcapture.core.domain.model

import android.graphics.PointF
import android.graphics.Rect

data class PaddleBox(
    val points: List<PointF>
) {
    fun toRect(imgW: Int, imgH: Int): Rect {
        val xs = points.map { (it.x * imgW).toInt() }
        val ys = points.map { (it.y * imgH).toInt() }

        return Rect(
            xs.minOrNull() ?: 0,
            ys.minOrNull() ?: 0,
            xs.maxOrNull() ?: 0,
            ys.maxOrNull() ?: 0
        )
    }
}