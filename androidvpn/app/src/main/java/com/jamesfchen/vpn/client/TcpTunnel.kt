package com.jamesfchen.vpn.client

import android.util.Log
import com.jamesfchen.vpn.Connection
import com.jamesfchen.vpn.ConnectionPool
import com.jamesfchen.vpn.client.PacketDispatcher.Companion.TAG
import com.jamesfchen.vpn.protocol.*
import okio.ByteString.Companion.toByteString
import java.net.ConnectException
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/08/2021  Fri
 */
class TcpTunnel(val pWriter: PacketWriter) : Runnable {
    var isusable:Boolean=false
    var syncCount = 0
    var packId = 1
    var seqNo=1
    var ackNo=0

    val donotConnect = mutableSetOf<String>()
    val connected = mutableSetOf<String>()
    val pool: ConnectionPool = ConnectionPool()
    private val packetQueue = ArrayBlockingQueue<Packet>(100)
    fun dispatch(packet: Packet) {
        packetQueue.offer(packet)
    }

    override fun run() {
        while (true) {
            val packet = packetQueue.take()
            val tcpHeader = packet.tlHeader as TcpHeader
            val sourceIp = packet.ipHeader.sourceAddresses.hostAddress
            val sourcePort = tcpHeader.sourcePort
            val destIp = packet.ipHeader.destinationAddresses.hostAddress
            val destPort = tcpHeader.destinationPort
            val srcAddr = InetSocketAddress(sourceIp, sourcePort)
            val destAddr = InetSocketAddress(destIp, destPort)
            val key = "${destIp}:${destPort}"
            val conn = try {
                if (destPort != 443) {
                    val (cc, isUsable) = pool.get(key)
                    val c = cc as Connection?
                    val sb = StringBuilder(
                        "vpn source:${sourceIp}:$sourcePort  dest:$key\t" +
                                "isUsable:$isUsable connecting socket local:${c?.localAddress} remote:${c?.remoteAddress}"
                    )
                    if (donotConnect.contains(destIp)) {
                        sb.append("\n")
                            .append("$key from port 443 down to port 80")
                        connected.add(destIp)
                    }
//                            Log.d(TAG, sb.toString())
//                            Log.d(TAG, "packet:$packet\n")
                    c
                } else {
//                            Log.d(TAG, "packet:$packet")
                    if (!connected.contains(destIp)) {
                        Log.i(TAG, "don't connect port 443 dest:$key")
                    }
                    donotConnect.add(destIp)
                    null
                }
            } catch (e: ConnectException) {
                Log.e(
                    TAG,
                    "error:" + e.message + "\t" + "source:${sourceIp}:$sourcePort  dest:$key\t"
                )
                null
            }
            val controlbit = tcpHeader.controlBit
            if (controlbit.hasSYN) {
                if (syncCount == 0) {
                    val seqNo = 1L
                    val ack = tcpHeader.sequenceNo + 1
                    val p = if (destPort != 443) {
                        createSynAndAckPacketOnHandshake(
                             destAddr,srcAddr, seqNo, ack, packId
                        )
                    } else {
                        createAckAndRSTPacketOnHandshake(
                             destAddr,srcAddr, seqNo, ack, packId
                        )
                    }
                    pWriter.writePacket(p)
                            Log.d(TAG, "$key syncCount$syncCount packId:$packId\npacket:$p")
                    ++packId
                } else {
                    val ack = tcpHeader.sequenceNo + 1
//                            Log.d(TAG, "$key syncCount$syncCount packId:$packId\n")
                }

                ++syncCount
            } else if (controlbit.hasACK) {//send data to remote
                Log.e(
                    TAG,
                    "req buffer remaining:${packet.payload?.remaining()}"
                )
                packet.payload?.let {
                    conn?.send(it) { respBuffer ->
                        Log.e(
                            TAG,
                            "resp buffer size:${respBuffer.remaining()} ${
                                respBuffer.toByteString().utf8()
                            }"
                        )
//                                pWriter.writePacket()
//
                    }
                }
            } else if (controlbit.hasFIN) {//close connection

            } else if (controlbit.hasPSH) {

            } else if (controlbit.hasRST) {

            } else if (controlbit.hasURG) {

            }
        }
    }
}