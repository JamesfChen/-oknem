package com.jamesfchen.vpn.client

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.jamesfchen.vpn.VpnHandlerThread
import com.jamesfchen.vpn.protocol.TcpFinException
import com.jamesfchen.vpn.protocol.TcpRstException
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/13/2021  Wed
 */
class BioSocketConnection(val socket: Socket) : Connection() {
    val outputStream = socket.getOutputStream()
    val inputStream = socket.getInputStream()
    override var remoteAddress: InetSocketAddress? =
        socket.remoteSocketAddress as InetSocketAddress?
    override var localAddress: InetSocketAddress? = socket.localSocketAddress as InetSocketAddress?

    companion object {
        const val BIO_TAG = "${C_TAG}/BioSocket"
    }

    override fun send(reqBuffer: ByteArray, block: (respBuffer: ByteBuffer) -> Unit) {
        val fileOutputStream =
            FileOutputStream(File("/storage/emulated/0/Android/data/com.jamesfchen.vpn/files/a/request.txt"))
        fileOutputStream.write(reqBuffer)
        fileOutputStream.flush()
        outputStream.write(reqBuffer)
        outputStream.flush()
        VpnHandlerThread.executor.submit {
            synchronized(this) {
                try {
                    while (true) {
                        val ba = ByteArray(16 * 1024)
                        val len = inputStream.read(ba)
                        if (len == -1) {
                            break
                        } else if (len == 0) {
                            Thread.sleep(50)
                        } else {
                            val wrap = ByteBuffer.wrap(ba)
                            wrap.limit(len)
                            block(wrap)
                        }
                    }
                } catch (e: java.io.IOException){
                    throw TcpRstException()
                }
                throw TcpFinException()
            }
        }.get()

    }

    override fun close() {
        outputStream.close()
        inputStream.close()
        socket.close()
        socket.shutdownInput()
        socket.shutdownOutput()
    }

}

class AioSocketConnection(
    val asynSocketChannel: AsynchronousSocketChannel
) : Connection() {
    companion object {
        const val AIO_TAG = "${C_TAG}/AioSocket"
    }

    override var remoteAddress: InetSocketAddress? =
        asynSocketChannel.remoteAddress as InetSocketAddress?
    override var localAddress: InetSocketAddress? =
        asynSocketChannel.localAddress as InetSocketAddress?

    @WorkerThread
    override fun send(reqBuffer: ByteArray, block: (respBuffer: ByteBuffer) -> Unit) {

        try {
            asynSocketChannel.write(ByteBuffer.wrap(reqBuffer)).get()
//            val byteBuffer: ByteBuffer =
//                ByteBuffer.allocate(com.jamesfchen.vpn.protocol.BUFFER_SIZE)
//            asynSocketChannel.read(byteBuffer).get()
//            byteBuffer.flip()
//            block(byteBuffer)
        } catch (e: Exception) {
            Log.d(AIO_TAG, Log.getStackTraceString(e))
        }
    }

    @MainThread
    override fun <A> send(
        reqBuffer: ByteArray,
        handler: CompletionHandler<Int, A?>,
        attachment: A?
    ) {

        try {
            asynSocketChannel.write(ByteBuffer.wrap(reqBuffer), attachment, handler)
//            val byteBuffer: ByteBuffer =
//                ByteBuffer.allocate(com.jamesfchen.vpn.protocol.BUFFER_SIZE)
//            asynSocketChannel.read(byteBuffer, attachment, handler)
//            byteBuffer.flip()
        } catch (e: Exception) {
            Log.d(AIO_TAG, Log.getStackTraceString(e))
        }
    }

    override fun close() {
        asynSocketChannel.close()
    }
}
