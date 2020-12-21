package com.jamesfchen.vpn

import android.util.Log
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/20/2020  Sun
 */
const val P_TAG="${Constants.TAG}/packet"
enum class PacketType {
    UNKNOWN,
    UDP,
    TCP
}

const val IP4_HEADER_SIZE = 20
const val TCP_HEADER_SIZE = 20
const val UDP_HEADER_SIZE = 8
const val BUFFER_SIZE= 16384

data class Header(val type: PacketType)
data class Packet(val header: Header, val buffer: ByteBuffer)

class PacketReader(private val vpnInput: FileChannel) : AutoCloseable, Closeable {
    init {
    }

    override fun close() {
        vpnInput.close()
    }

    fun nextPacket(): Packet {
        val header = Header(PacketType.TCP)
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        vpnInput.read(buffer)
        buffer.flip()
        Log.d(P_TAG,"read buffet${buffer}")
        return Packet(header, buffer)
    }

}

class PacketWriter(private val vpnOutput: FileChannel) : AutoCloseable, Closeable {


    init {
    }

    override fun close() {
        vpnOutput.close()
    }

    fun writePacket(p:Packet) {
    }

}