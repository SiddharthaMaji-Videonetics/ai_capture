package com.videonatics.smartcapture.core

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class BackendWebSocket(
    private val onResult: (String) -> Unit
) {

    private val client = OkHttpClient()

    private lateinit var webSocket: WebSocket

    fun connect() {

        val request = Request.Builder()
            .url("ws://YOUR_BACKEND_URL")
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {
                    onResult(text)
                }
            }
        )
    }

    fun sendFrame(bytes: ByteArray) {
        //webSocket.send(ByteString.of(*bytes))
    }
}
