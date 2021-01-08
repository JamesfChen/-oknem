package com.jamesfchen.vpn.protocol

import android.os.ParcelFileDescriptor
import android.util.Log
import com.jamesfchen.vpn.Constants
import com.jamesfchen.vpn.getUByte
import com.jamesfchen.vpn.getUShort
import okhttp3.internal.toHexString
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*

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

data class Packet(
    val ipHeader: IpHeader,
    var tlHeader: TransportLayerHeader? = null,
    val payload: ByteBuffer? = null
) {
    override fun toString(): String {
        return "Packet(ipHeader=$ipHeader, tlHeader=$tlHeader,payload:${payload?.remaining()})"
    }

    fun toByteBuffer(): ByteBuffer {
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

    /** 对于报文的发送者(最后计算得到的checksum为一个有效值)
     * 1. 将checksum设置为0
     * 2. 将pesudo header 的sum + tcp header sum + tcp payload sum
     * 3. 取sum反码
     *
     * 对于报文的接受者(最后计算的checksum为0)
     * 1. 将pesudo header 的sum + tcp header sum + tcp payload sum
     * 2. 取sum反码
     *
     */
    fun computeChecksum(): Int {
        //pseudoHeaderSum
        var sum = 0
        var len = when (ipHeader.protocol) {
            Protocol.TCP -> {
                (tlHeader as TcpHeader).headerLen + (payload?.remaining() ?: 0)
            }
            Protocol.UDP -> {
                8
            }
            else -> 0
        }
        // Calculate pseudo-header checksum
        var buffer = ByteBuffer.wrap(ipHeader.sourceAddresses.address)
        sum += buffer.getUShort() + buffer.getUShort()
        buffer = ByteBuffer.wrap(ipHeader.destinationAddresses.address)
        sum += buffer.getUShort() + buffer.getUShort()
        sum += Protocol.TCP.typeValue + len
        // Calculate TCP segment checksum
        val b = toByteBuffer()
        b.position(IP4_HEADER_SIZE)
        while (len > 1) {
            val e = b.getUShort()
            sum += e
            len -= 2
        }
        if (len > 0) sum += b.getUByte() shl 8
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
//        sum = 0xff_ff - sum//取反   sum.inv()取反有问题
        sum=sum.inv()
        return sum
    }
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
    val p = Packet(ipHeader, tlHeader, this)
    val computeChecksum = p.computeChecksum()
    if (computeChecksum == 0) {//从流中读取到一个packet，作为接受者，计算checksum应该为0

    }
    val cs = (tlHeader as? TcpHeader)?.checksum ?: 0
    println("tcp checksum : compute=$computeChecksum ${cs}")
    return p
}

fun createTCPPacket(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long,
    controlBit: ControlBit, ipId: Int, payload: ByteBuffer? = null
): Packet {
    val tlHeader = TcpHeader(
        sourcePort = sour.port, destinationPort = dest.port,
        sequenceNo = seq, acknowledgmentNo = ack,
        dataOffset = 5, controlBit = controlBit,
        window = 65535, urgentPointer = 0
    )
    val totalLength = IP4_HEADER_SIZE + tlHeader.headerLen + (payload?.remaining() ?: 0)

    val ipHeader = IpHeader(
        IpVersion.V4, 5,
        TypeOfService(0), totalLength = totalLength,
        identification = ipId, flags = IpFlag(0b010/*不分片*/), fragmentOffset = 0,
        ttl = 64,
        protocol = Protocol.TCP,
        sourceAddresses = sour.address, destinationAddresses = dest.address
    )
    ipHeader.headerChecksum = ipHeader.computeHeaderChecksum()

    val p = Packet(ipHeader, tlHeader, ByteBuffer.wrap(byteArrayOf()))
    tlHeader.checksum = p.computeChecksum()
    return p
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
        Log.e(P_TAG, "packet reader closed")
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
//            Log.d(P_TAG, "buffer:${buffer}  len:${len} ")
            val packet = buffer.getPacket()
//            Log.d(P_TAG, "read buffet: $packet")
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
        Log.e(P_TAG, "packet writer closed")
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
