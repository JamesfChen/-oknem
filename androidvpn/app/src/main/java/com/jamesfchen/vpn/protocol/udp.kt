package com.jamesfchen.vpn.protocol

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.jamesfchen.vpn.Constants
import com.jamesfchen.vpn.getUShort
import com.jamesfchen.vpn.putUShort
import java.nio.ByteBuffer

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/19/2020  Sat
 * [User Datagram Protocol](https://tools.ietf.org/html/rfc768)
 */
const val U_TAG = "${Constants.TAG}/udp"
const val UDP_HEADER_SIZE = 8

class UdpHandlerThread() : HandlerThread("udp_thread") {

}

class UdpHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        Log.d(U_TAG, "udp message")
    }

}

fun ByteBuffer.getUdpHeader(): UdpHeader {
    val sourPort = getUShort()
    val destPort = getUShort()
    val len = getUShort()
    val checksum = getUShort()
    return UdpHeader(sourPort, destPort, len, checksum)
}

data class UdpHeader(
    val sourcePort: Int,
    val destPort: Int,
    val udpLen: Int,
    val checksum: Int
) : TransportLayerHeader {
    /*

                  0      7 8     15 16    23 24    31
                 +--------+--------+--------+--------+
                 |     Source      |   Destination   |
                 |      Port       |      Port       |
                 +--------+--------+--------+--------+
                 |                 |                 |
                 |     Length      |    Checksum     |
                 +--------+--------+--------+--------+
                 |
                 |          data octets ...
                 +---------------- ...

                      User Datagram Header Format
     */
    override fun toByteBuffer() = ByteBuffer.allocate(UDP_HEADER_SIZE).apply {
        .putUShort(sourcePort)
        .putUShort(destPort)
        .putUShort(udpLen)
        .putUShort(checksum)
        .flip()
    }
}
