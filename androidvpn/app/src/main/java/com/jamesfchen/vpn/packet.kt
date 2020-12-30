package com.jamesfchen.vpn

import android.system.Os
import android.util.Log
import com.jamesfchen.vpn.protocol.*
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/20/2020  Sun
 *
Protocol Layering

+---------------------+
|     higher-level    |
+---------------------+
|        TCP          |
+---------------------+
|  internet protocol  |
+---------------------+
|communication network|
+---------------------+
 */

const val P_TAG = "${Constants.TAG}/packet"
const val BUFFER_SIZE = 16384//65535=2^15-1

interface TransportLayerHeader
data class Packet(val buffer: ByteBuffer) {
    val ipHeader: IpHeader = IpHeader(buffer)
    var tlHeader: TransportLayerHeader? = null
    val payload: ByteBuffer

    init {
        when (ipHeader.protocol) {
            Protocol.TCP -> {
                tlHeader = TcpHeader(buffer)
            }
            Protocol.UDP -> {
                tlHeader = UdpHeader(buffer)
            }
            else -> {
            }
        }
        payload = buffer
    }

    override fun toString(): String {
        return "Packet(ipHeader=$ipHeader, tlHeader=$tlHeader)"
    }

}

class PacketReader(private val vpnInput: FileChannel) : AutoCloseable, Closeable {
    val buffer = ByteBuffer.allocate(BUFFER_SIZE)

    init {
    }

    override fun close() {
        Log.d(P_TAG, "packet reader closed")
        vpnInput.close()
    }

    fun nextPacket(): Packet? {
        var remaining = 0
        try {
            remaining = vpnInput.read(buffer)
        } catch (e: IOException) {
            Log.d(P_TAG, Log.getStackTraceString(e))
        }
        if (remaining == 0) {
            return null
        }
        Log.d(
            P_TAG,
            "limit:${buffer.limit()} capacity:${buffer.capacity()}  remaining:${remaining}"
        )
        buffer.flip()
        val packet = Packet(buffer)
        Log.d(
            P_TAG,
            "read buffet: ${packet}"
        )
        return packet
    }

}

class PacketWriter(private val vpnOutput: FileChannel) : AutoCloseable, Closeable {


    init {
    }

    override fun close() {
        vpnOutput.close()
    }

    fun writePacket(p: Packet) {
    }

}