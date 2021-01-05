package com.jamesfchen.vpn.protocol

import android.os.ParcelFileDescriptor
import android.util.Log
import com.jamesfchen.vpn.Constants
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
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

fun createTCPPacket(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long,
    controlBit: ControlBit, ipId: Int
): Packet {
    val ipHeader = IpHeader(
        IpVersion.V4, 20,
        TypeOfService(0), totalLength = 60,
        identification = ipId, flags = IpFlag(0b010), fragmentOffset = 0,
        ttl = 64,
        protocol = Protocol.TCP,
        headerChecksum = 0,
        sourceAddresses = sour.address, destinationAddresses = dest.address
    )
    val tlHeader = TcpHeader(
        sourcePort = sour.port, destinationPort = dest.port,
        sequenceNo = seq, acknowledgmentNo = ack,
        dataOffset = 40, controlBit = controlBit,
        window = 65535, checksum = 0, urgentPointer = 0
    )
    tlHeader.optionsAndPadding = ByteArray(20)

    return Packet(ipHeader, tlHeader, ByteBuffer.wrap(byteArrayOf()))
}

//fun createSynPacketOnHandshake():Packet{
//}
fun createAckAndRSTPacketOnHandshake(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long, ipId: Int
): Packet {
    return createTCPPacket(
        sour, dest, seq, ack, ControlBit(ControlBit.RST or ControlBit.ACK), ipId
    )
}

fun createSynAndAckPacketOnHandshake(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long, ipId: Int
): Packet {
    return createTCPPacket(
        sour, dest, seq, ack, ControlBit(ControlBit.SYN or ControlBit.ACK), ipId
    )
}

fun createAckPacketOnHandshake(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long, ipId: Int
): Packet {
//    return createTCPPacket(
//        InetSocketAddress("10.0.0.2", 41892),
//        InetSocketAddress("59.111.181.60", 443),
//        1, 1,
//        ControlBit(ControlBit.SYN or ControlBit.ACK),
//        1
//    )
    return createTCPPacket(
        sour, dest,
        seq, ack,
        ControlBit(ControlBit.ACK),
        ipId
    )
}

fun createAckPacketOnWave(
    sour: InetSocketAddress, dest: InetSocketAddress,
    seq: Long, ack: Long, ipId: Int
): Packet {
    return createTCPPacket(
        sour, dest,
        seq, ack,
        ControlBit(ControlBit.ACK),
        ipId
    )
}

fun createFinPacketOnWave(
    sour: InetSocketAddress, dest: InetSocketAddress,
    seq: Long, ack: Long, ipId: Int
): Packet {
    return createTCPPacket(
        sour, dest,
        seq, ack,
        ControlBit(ControlBit.FIN),
        ipId
    )
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
