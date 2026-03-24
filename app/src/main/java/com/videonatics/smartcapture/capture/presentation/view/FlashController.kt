package com.videonatics.smartcapture.capture.presentation.view

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FlashController(context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraId: String? = null

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn

    init {
        cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val hasFlash =
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val lensFacing =
                characteristics.get(CameraCharacteristics.LENS_FACING)
            hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
        }

        registerTorchCallback()
    }

    private fun registerTorchCallback() {
        cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {

            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) {
                    _isFlashOn.value = enabled
                }
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) {
                    _isFlashOn.value = false
                }
            }

        }, null)
    }

    fun setFlash(on: Boolean) {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, on)
            } catch (e: CameraAccessException) {
                Log.e("FlashController", "Failed to set torch mode: ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("FlashController", "Camera ID not found or doesn't support torch: ${e.message}")
            }
        }
    }
}