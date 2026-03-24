package com.videonatics.smartcapture.core.ar_core

import android.content.Context
import com.google.ar.core.Config
import com.google.ar.core.Session

object ARCoreManager {

    var session: Session? = null

    fun init(context: Context) {
        if (session != null) return

        session = Session(context)

        val config = Config(session).apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            depthMode = Config.DepthMode.DISABLED // 🔥 disable for now
        }

        session?.configure(config)
    }

    fun resume() {
        session?.resume()
    }

    fun pause() {
        session?.pause()
    }

    fun destroy() {
        session?.close()
        session = null
    }
}