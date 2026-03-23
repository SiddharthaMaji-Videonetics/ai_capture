package com.videonatics.smartcapture.core

import android.content.Context
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException
import com.videonatics.smartcapture.capture.domain.model.ARFrameHolder

object ARCoreManager {

    var session: Session? = null
    @Volatile private var isRunning = false
    @Volatile private var isResumed = false

    fun init(context: Context) {
        if (session != null) return

        try {
            session = Session(context)

            val config = Config(session).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                depthMode = Config.DepthMode.AUTOMATIC
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            }

            session?.configure(config)
        } catch (e: Exception) {
            Log.e("ARCoreManager", "Failed to initialize ARCore session", e)
        }
    }

    fun start() {
        if (isRunning) return

        isRunning = true

        Thread {
            while (isRunning) {

                if (!isResumed) {
                    try {
                        Thread.sleep(50)
                    } catch (e: InterruptedException) {
                        break
                    }
                    continue
                }

                try {
                    val currentSession = session
                    if (currentSession != null) {
                        val frame = currentSession.update()
                        ARFrameHolder.latestFrame = frame
                    }
                } catch (e: Exception) {
                    // avoid crash spam, but log it occasionally or if critical
                    // Log.e("ARCoreManager", "Update failed", e)
                }

                try {
                    Thread.sleep(16)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.start()
    }

    fun resume() {
        try {
            session?.resume()
            isResumed = true
        } catch (e: Exception) {
            Log.e("ARCoreManager", "Failed to resume ARCore session", e)
            isResumed = false
        }
    }

    fun pause() {
        isResumed = false
        try {
            session?.pause()
        } catch (e: Exception) {
            Log.e("ARCoreManager", "Failed to pause ARCore session", e)
        }
    }

    fun stop() {
        isRunning = false
        isResumed = false
        try {
            session?.pause()
            session?.close()
        } catch (e: Exception) {
            Log.e("ARCoreManager", "Failed to stop ARCore session", e)
        }
        session = null
    }
}