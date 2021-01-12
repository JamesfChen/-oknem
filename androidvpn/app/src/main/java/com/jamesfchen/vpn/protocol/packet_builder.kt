package com.jamesfchen.vpn.protocol

import android.util.Log
import com.jamesfchen.vpn.Constants.sInterceptIps
import java.net.InetSocketAddress

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/12/2021  Tue
 */
var isFirstSyncACK = true
var isFirstRSTACK = true
var isFirstACK = true
var isFirstRST = true
fun createTCPPacket(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long,
    controlBit: ControlBit, ipId: Int, payload: ByteArray? = null
): Packet {

    val tlHeader = TcpHeader(
        sourcePort = sour.port, destinationPort = dest.port,
        sequenceNo = seq, acknowledgmentNo = ack,
        dataOffset = 5, controlBit = controlBit,
        window = 65535, urgentPointer = 0
    )
    val totalLength = IP4_HEADER_SIZE + tlHeader.headerLen + (payload?.size ?: 0)

    val ipHeader = IpHeader(
        IpVersion.V4, 5,
        TypeOfService(0), totalLength = totalLength,
        identification = ipId, flags = IpFlag(0b010/*不分片*/), fragmentOffset = 0,
        ttl = 64,
        protocol = Protocol.TCP,
        sourceAddresses = sour.address, destinationAddresses = dest.address
    )
    ipHeader.headerChecksum = ipHeader.computeHeaderChecksum()
    val p = Packet(ipHeader, tlHeader, payload)
    tlHeader.checksum = p.computeChecksum()
    if (sInterceptIps.contains(ipHeader.sourceAddresses.hostAddress)) {
        if (isFirstSyncACK && controlBit.hasSYN && controlBit.hasACK) {
            Log.d("cjfvpn/dispatcher", "<<< send  server's sync_ack packet$p")
            isFirstSyncACK = false
        } else if (isFirstRSTACK && controlBit.hasRST && controlBit.hasACK) {
            isFirstRSTACK = false
            Log.d("cjfvpn/dispatcher", "<<< send  server's rst_ack packet$p")
        } else if (isFirstACK && controlBit.hasACK) {
            isFirstACK = false
            Log.d("cjfvpn/dispatcher", "<<< send  server's ack packet$p")

        } else if (isFirstRST && controlBit.hasRST) {
            isFirstRST=false
            Log.d("cjfvpn/dispatcher", "<<< send  server's rst packet$p")
        }
    }
    return p
}

//fun createSynPacketOnHandshake():Packet{
//}
fun createRSTPacketOnHandshake(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long, ipId: Int
): Packet {
    return createTCPPacket(
        sour, dest, seq, ack, ControlBit(ControlBit.RST), ipId
    )
}
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
fun createAckPacketOnDataExchange(
    sour: InetSocketAddress, dest: InetSocketAddress, seq: Long, ack: Long, ipId: Int,payload: ByteArray? = null
): Packet {
    return createTCPPacket(
        sour, dest,
        seq, ack,
        ControlBit(ControlBit.ACK),
        ipId,payload
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