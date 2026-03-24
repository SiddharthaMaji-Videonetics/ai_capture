package com.videonatics.smartcapture.core.ar_core

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException

object ARCoreManager {

    var session: Session? = null
        private set

    fun init(context: Context) {
        if (session != null) return

        try {
            session = Session(context)
            val session = session ?: return

            //  STEP 1: Select BEST camera config (before configure)
            val cameraConfigs = session.getSupportedCameraConfigs()

            val display = (context as Activity).windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)

            val screenAspect = metrics.widthPixels.toFloat() / metrics.heightPixels

            val bestConfig = cameraConfigs.minByOrNull { config ->
                val image = config.imageSize
                val aspect = image.width.toFloat() / image.height

                val aspectDiff = kotlin.math.abs(aspect - screenAspect)

                // prioritize aspect match first, then resolution
                aspectDiff * 10000 - (image.width * image.height)
            }

            if (bestConfig != null) {
                session.cameraConfig = bestConfig

                Log.d(
                    "ARCore",
                    "Selected camera: ${bestConfig.imageSize.width}x${bestConfig.imageSize.height}"
                )
            }

            // 🔥 STEP 2: Depth support
            val isDepthSupported =
                session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)

            Log.d("ARCore", "Depth supported: $isDepthSupported")

            val config = Config(session).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                depthMode = if (isDepthSupported) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }

                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            }

            session.configure(config)

        } catch (e: UnavailableException) {
            Log.e("ARCore", "ARCore not available", e)
        } catch (e: Exception) {
            Log.e("ARCore", "Session init failed", e)
        }
    }

    fun resume() {
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e("ARCore", "Camera not available on resume", e)
            session = null
        }
    }

    fun pause() {
        try {
            session?.pause()
        } catch (e: Exception) {
            Log.e("ARCore", "Pause failed", e)
        }
    }

    fun destroy() {
        try {
            session?.close()
        } catch (e: Exception) {
            Log.e("ARCore", "Destroy failed", e)
        } finally {
            session = null
        }
    }
}