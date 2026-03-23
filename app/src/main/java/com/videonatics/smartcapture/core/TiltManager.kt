package com.videonatics.smartcapture.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TiltSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _tilt = MutableStateFlow(Tilt(0f, 0f))
    val tilt: StateFlow<Tilt> = _tilt

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

            _tilt.value = Tilt(pitch, roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

data class Tilt(
    val pitch: Float,
    val roll: Float
)