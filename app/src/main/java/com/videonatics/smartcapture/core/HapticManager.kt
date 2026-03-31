package com.videonatics.smartcapture.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

class HapticManager(private val context: Context) {

    private val vibrator =
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    enum class Type {
        SUCCESS,
        ERROR,
        WARNING
    }

    fun perform(type: Type, view: View? = null) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            view?.let {
                when (type) {
                    Type.SUCCESS -> it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    Type.ERROR -> it.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    Type.WARNING -> it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }


            val pattern = when (type) {
                Type.SUCCESS -> longArrayOf(0, 20, 30, 20)      // soft double tick
                Type.ERROR -> longArrayOf(0, 40, 20, 40, 20, 60) // stronger triple
                Type.WARNING -> longArrayOf(0, 30, 40, 30)       // medium double
            }

            vibrate(pattern)
        }

    }

    private fun vibrate(pattern: LongArray) {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }
}