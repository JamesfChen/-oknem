package com.jamesfchen.vpn

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.jamesfchen.vpn.protocol.*
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
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

//const val BUFFER_SIZE = 65535//65535=2^15-1
interface TransportLayerHeader {
    fun toByteBuffer(): ByteBuffer
}

fun ByteBuffer.getPacket(): Packet {
    val ipHeader = getIpHeader()
    var tlHeader: TransportLayerHeader? = null
    when (ipHeader.protocol) {
        Protocol.TCP -> {
            tlHeader = getTcpHeader()
        }
        Protocol.UDP -> {
            tlHeader = getUdpHeader()
        }
        else -> {
        }
    }
    return Packet(ipHeader, tlHeader, this)
}

fun Packet.toByteBuffer(): ByteBuffer {
    val ipHeaderBuffer = ipHeader.toByteBuffer()
    val tlHeaderBuffer = tlHeader?.toByteBuffer()
    val totalBuffer = ByteBuffer.allocate(ipHeader.totalLength)
    totalBuffer.put(ipHeaderBuffer)
    tlHeaderBuffer?.let {
        totalBuffer.put(it)
    }
    payload?.let {
        totalBuffer.put(it)
    }
    totalBuffer.flip()
    return totalBuffer
}

data class Packet(
    val ipHeader: IpHeader,
    var tlHeader: TransportLayerHeader? = null,
    val payload: ByteBuffer? = null
) {
    override fun toString(): String {
        return "Packet(ipHeader=$ipHeader, tlHeader=$tlHeader,payload:${payload?.remaining()})"
    }
}


class PacketReader constructor(vpnInterface: ParcelFileDescriptor) : AutoCloseable, Closeable {
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)
    private val vpnfd = vpnInterface.fileDescriptor
    private val vpnInputChannel: FileChannel = FileInputStream(vpnfd).channel
    private val vpnInput = FileInputStream(vpnfd)
    override fun close() {
        Log.d(P_TAG, "packet reader closed")
        vpnInputChannel.close()
    }

    fun nextPacket(): Packet? {
        try {
            buffer.clear()
            val len = vpnInputChannel.read(buffer)
            if (len == 0) {
                return null
            }
            buffer.flip()
            buffer.limit(len)
            Log.d(P_TAG, "buffer:${buffer}  len:${len} ")
            val packet = buffer.getPacket()
            Log.d(P_TAG, "read buffet: $packet")
            return packet
        } catch (e: Exception) {
            Log.d(P_TAG, Log.getStackTraceString(e))
            return null
        }
    }

}

class PacketWriter(vpnInterface: ParcelFileDescriptor) : AutoCloseable, Closeable {
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)
    private val vpnfd = vpnInterface.fileDescriptor
    private val vpnOutputChannel: FileChannel = FileOutputStream(vpnfd).channel

    override fun close() {
        Log.d(P_TAG, "packet writer closed")
        vpnOutputChannel.close()
    }

    fun writePacket(p: Packet) {
        try {
            vpnOutputChannel.write(p.toByteBuffer())
        } catch (e: Exception) {
            Log.d(P_TAG, Log.getStackTraceString(e))
        }
    }

}