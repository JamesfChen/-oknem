package com.jamesfchen.vpn.protocol

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.jamesfchen.vpn.Constants
import com.jamesfchen.vpn.TransportLayerHeader
import java.nio.Buffer
import java.nio.ByteBuffer

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/19/2020  Sat
 */
const val U_TAG="${Constants.TAG}/udp"
const val UDP_HEADER_SIZE = 8
class UdpHandlerThread() : HandlerThread("udp_thread") {

}

class UdpHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        Log.d(U_TAG,"udp message")
    }

}
fun ByteBuffer.getUdpHeader(): UdpHeader {
    return UdpHeader(1)
}
data class UdpHeader(val v:Int):TransportLayerHeader {
    override fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(10)
        return buffer
    }
}