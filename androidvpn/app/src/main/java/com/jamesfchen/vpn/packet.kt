package com.jamesfchen.vpn

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
enum class PacketType {
    UNKNOWN,
    UDP,
    TCP
}

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
        val buffer = ByteBuffer.allocate(1024)
//        vpnInput.read(buffer)
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