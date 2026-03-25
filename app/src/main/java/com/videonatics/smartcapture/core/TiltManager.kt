package com.videonatics.smartcapture.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.videonatics.smartcapture.feature.capture.domain.model.Tilt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class TiltSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // --- CONFIG ---
    private val tiltThreshold = 5f           // degrees
    private val stabilityTimeMs = 500L       // must be stable for 500ms
    private val alpha = 0.1f                 // smoothing factor

    // --- STATE ---
    private val _tilt = MutableStateFlow(Tilt())
    val tilt: StateFlow<Tilt> = _tilt

    private var smoothPitch = 0f
    private var smoothRoll = 0f

    private var lastStableTime = 0L
    private var isStarted = false

    fun start() {
        if (isStarted) return
        rotationSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME // smoother than UI
            )
            isStarted = true
        }
    }

    fun stop() {
        if (!isStarted) return
        sensorManager.unregisterListener(this)
        isStarted = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        // Extract gravity vector
        val gX = rotationMatrix[6]
        val gY = rotationMatrix[7]
        val gZ = rotationMatrix[8]

        // Compute pitch & roll from gravity (more stable)
        val pitchRaw = Math.toDegrees(atan2(-gX.toDouble(), sqrt((gY * gY + gZ * gZ).toDouble()))).toFloat()
        val rollRaw = Math.toDegrees(atan2(gY.toDouble(), gZ.toDouble())).toFloat()

        // smoothing (keep your existing)
        smoothPitch += alpha * (pitchRaw - smoothPitch)
        smoothRoll += alpha * (rollRaw - smoothRoll)

        val isLevelNow =
            kotlin.math.abs(smoothPitch) < tiltThreshold &&
                    kotlin.math.abs(smoothRoll) < tiltThreshold

        val currentTime = System.currentTimeMillis()

        val isStable = if (isLevelNow) {
            if (lastStableTime == 0L) lastStableTime = currentTime
            currentTime - lastStableTime > stabilityTimeMs
        } else {
            lastStableTime = 0L
            false
        }

        _tilt.value = Tilt(
            pitch = smoothPitch,
            roll = smoothRoll,
            isLevel = isLevelNow,
            isStable = isStable
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}