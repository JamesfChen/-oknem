package com.jamesfchen.vpn

import android.util.Log
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Future

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/20/2020  Sun
 */
const val C_TAG="${Constants.TAG}/client"
class AioSocketClient {
    companion object {
//        private const val PORT = 8888
//        private const val IP_ADDRESS = "127.0.0.1"
    }

    private val asynSocketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()

    fun connect(ip: String, port: Int): Future<Void> {
        return asynSocketChannel.connect(InetSocketAddress(ip, port))
    }

    fun send(reqBuffer: ByteBuffer, block: (respBuffer: ByteBuffer) -> Unit) {
        try {
            asynSocketChannel.write(reqBuffer).get()
            val byteBuffer: ByteBuffer = ByteBuffer.allocate(1024)
            asynSocketChannel.read(byteBuffer).get()
            byteBuffer.flip()
            block(byteBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
class MyWebSocketListener : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
//        ByteBuffer
//        IntBuffer
//        StringBuffer
        Log.d(C_TAG,"onMessage bytes:$bytes")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        Log.d(C_TAG,"onMessage text:$text")
    }
}