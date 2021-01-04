package com.jamesfchen.vpn

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/20/2020  Sun
 */
const val C_TAG = "${Constants.TAG}/cli"

interface Client {
    companion object {
        fun createAndConnect(ip: String, port: Int, aioSocket: Boolean): Client {
            if (aioSocket) {
                val asynSocketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
                asynSocketChannel.connect(InetSocketAddress(ip, port)).get()
                return AioSocketClient(asynSocketChannel)
            } else {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port))
                return BioSocketClient(socket)
            }
        }

        fun <A> createAndConnect(
            ip: String,
            port: Int,
            handler: CompletionHandler<Void, A?>,
            attachment: A? = null
        ): Client {
            val asynSocketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
            asynSocketChannel.connect(InetSocketAddress(ip, port), attachment, handler)
            return AioSocketClient(asynSocketChannel)

        }

    }

    // -- asynchronous operations --

    val remoteAddress: SocketAddress?
    val localAddress: SocketAddress?
    fun send(reqBuffer: ByteBuffer, block: (respBuffer: ByteBuffer) -> Unit)
    fun <A> send(
        reqBuffer: ByteBuffer,
        handler: CompletionHandler<Int, A?>,
        attachment: A? = null
    ) {
    }
}

class BioSocketClient(val socket: Socket) : Client {
    val outputStream = socket.getOutputStream()
    val inputStream = socket.getInputStream()
    override var remoteAddress: SocketAddress? = socket.remoteSocketAddress
    override var localAddress: SocketAddress? = socket.localSocketAddress

    companion object {
        const val BIO_TAG = "${C_TAG}/BioSocket"
    }

    override fun send(reqBuffer: ByteBuffer, block: (respBuffer: ByteBuffer) -> Unit) {
        try {
            outputStream.write(reqBuffer.array())
            val ba = ByteArray(com.jamesfchen.vpn.protocol.BUFFER_SIZE)
            val len = inputStream.read(ba)
            val byteBuffer = if (len > 0) {
                val wrap = ByteBuffer.wrap(ba)
                wrap.limit(len)
                wrap
            } else {
                ByteBuffer.wrap("".toByteArray())
            }
            block(byteBuffer)
        } catch (e: Exception) {
            Log.d(BIO_TAG, Log.getStackTraceString(e))
        }
    }

}

class AioSocketClient(
    val asynSocketChannel: AsynchronousSocketChannel
) : Client {
    companion object {
        const val AIO_TAG = "${C_TAG}/AioSocket"
    }

    override var remoteAddress: SocketAddress? = asynSocketChannel.remoteAddress
    override var localAddress: SocketAddress? = asynSocketChannel.localAddress

    @WorkerThread
    override fun send(reqBuffer: ByteBuffer, block: (respBuffer: ByteBuffer) -> Unit) {

        try {
            asynSocketChannel.write(reqBuffer).get()
            val byteBuffer: ByteBuffer = ByteBuffer.allocate(com.jamesfchen.vpn.protocol.BUFFER_SIZE)
            asynSocketChannel.read(byteBuffer).get()
            byteBuffer.flip()
            block(byteBuffer)
        } catch (e: Exception) {
            Log.d(AIO_TAG, Log.getStackTraceString(e))
        }
    }

    @MainThread
    override fun <A> send(
        reqBuffer: ByteBuffer,
        handler: CompletionHandler<Int, A?>,
        attachment: A?
    ) {

        try {
            asynSocketChannel.write(reqBuffer, attachment, handler)
            val byteBuffer: ByteBuffer = ByteBuffer.allocate(com.jamesfchen.vpn.protocol.BUFFER_SIZE)
            asynSocketChannel.read(byteBuffer, attachment, handler)
            byteBuffer.flip()
        } catch (e: Exception) {
            Log.d(AIO_TAG, Log.getStackTraceString(e))
        }
    }
}

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