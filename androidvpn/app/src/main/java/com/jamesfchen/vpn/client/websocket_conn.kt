package com.jamesfchen.vpn.client

import android.util.Log
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/13/2021  Wed
 */
class MyWebSocketListener : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
//        ByteBuffer
//        IntBuffer
//        StringBuffer
        Log.d(C_TAG, "onMessage bytes:$bytes")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        Log.d(C_TAG, "onMessage text:$text")
    }
}
