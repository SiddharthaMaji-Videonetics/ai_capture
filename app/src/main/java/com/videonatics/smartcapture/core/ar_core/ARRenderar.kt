package com.videonatics.smartcapture.core.ar_core

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.videonatics.smartcapture.core.isFlatInBoundary
import com.videonatics.smartcapture.core.isSceneFlat
import com.videonatics.smartcapture.ui.BoundaryPercent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(
    private val context: Context,
    private val onFlatDetected: (Boolean) -> Unit
) : GLSurfaceView.Renderer {
    private val resultHistory = ArrayDeque<Boolean>()

    private val backgroundRenderer = BackgroundRenderer()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        ARCoreManager.init(context)
        try {
            ARCoreManager.session?.resume()
        } catch (e: Exception) {
            Log.e("AR", "Resume failed", e)
        }

        backgroundRenderer.createOnGlThread(context)

        ARCoreManager.session?.setCameraTextureName(
            backgroundRenderer.textureId
        )
    }
    private var viewportWidth = 0
    private var viewportHeight = 0

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height

        val rotation = (context as Activity)
            .windowManager.defaultDisplay.rotation

        ARCoreManager.session?.setDisplayGeometry(
            rotation,
            width,
            height
        )
    }

    override fun onDrawFrame(gl: GL10?) {

        val session = ARCoreManager.session ?: return

        try {
            val frame = session.update()

            val display = (context as Activity).windowManager.defaultDisplay
            session.setDisplayGeometry(
                display.rotation,
                viewportWidth,
                viewportHeight
            )


            // draw camera correctly
            backgroundRenderer.draw(frame)
            if (isReady(frame)) {
                Log.e("AR_Process", "Tracking: ${frame.camera.trackingState}")
                val raw = isFlatInBoundary(
                    frame,
                    boundary = BoundaryPercent()
                )

                val stable = getStableResult(raw)

                onFlatDetected(stable)
            }



        } catch (e: Exception) {
            Log.e("ARRenderer", "error", e)
        }
    }

    fun getStableResult(current: Boolean): Boolean {

        resultHistory.addLast(current)

        if (resultHistory.size > 10) {
            resultHistory.removeFirst()
        }

        val flatCount = resultHistory.count { it }

        return flatCount >= 7 //  threshold tuning
    }
}

private var startTime = System.currentTimeMillis()

fun isReady(frame: Frame): Boolean {
    val ready = frame.camera.trackingState == TrackingState.TRACKING &&
            System.currentTimeMillis() - startTime > 1000

    return ready
}

