package com.jamesfchen.vpn

import android.os.Handler
import android.util.Log
import com.jamesfchen.vpn.client.Connection
import com.jamesfchen.vpn.client.ConnectionPool
import com.jamesfchen.vpn.protocol.Packet
import com.jamesfchen.vpn.protocol.UdpHeader
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/12/2021  Tue
 */

class UdpAnswerer(
    val pWriter: PacketWriter,
    val answerHandler: Handler,
    val answerers: ConcurrentHashMap<String, UdpAnswerer>
) : Runnable {
    var isusable: Boolean = false
    fun dispatch(packet: Packet) {
        packetQueue.offer(packet)
    }

    private val packetQueue = ArrayBlockingQueue<Packet>(500)
    private val connPool = ConnectionPool
    override fun run() {
        while (true) {
            val packet = packetQueue.take()
            val udpHeader = packet.tlHeader as UdpHeader
            val sourceIp = packet.ipHeader.sourceAddresses.hostAddress
            val sourcePort = udpHeader.sourcePort
            val destIp = packet.ipHeader.destinationAddresses.hostAddress
            val destPort = udpHeader.destPort
            val srcAddr = InetSocketAddress(sourceIp, sourcePort)
            val destAddr = InetSocketAddress(destIp, destPort)
            val key = "${destIp}:${destPort}"

            val (cc, isUsable) = connPool.getOverUdp(key)
            val conn = cc as Connection
            val payloadSize = packet.payload?.size ?: 0
            if (payloadSize == 0) {
                continue
            }
//            Log.d(TAG, "udp ${packet}")
//            Log.d("udp","size:$ip:$port")
            conn.send(packet.payload!!) {

            }

        }

    }

}