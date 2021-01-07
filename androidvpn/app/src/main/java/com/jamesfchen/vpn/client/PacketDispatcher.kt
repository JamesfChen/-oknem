package com.jamesfchen.vpn.client

import android.util.Log
import com.jamesfchen.vpn.Connection
import com.jamesfchen.vpn.ConnectionPool
import com.jamesfchen.vpn.Constants
import com.jamesfchen.vpn.protocol.*
import com.jamesfchen.vpn.threadFactory
import okio.ByteString.Companion.toByteString
import java.lang.StringBuilder
import java.net.ConnectException
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Jan/05/2021  Tue
 */
class PacketDispatcher {
    companion object {
        const val TAG = "${Constants.TAG}/dispatcher"

    }

    val donotConnect = mutableSetOf<String>()
    val connected = mutableSetOf<String>()
    val pool: ConnectionPool = ConnectionPool()
    private val executor = ThreadPoolExecutor(
        0, Int.MAX_VALUE, 60L, TimeUnit.SECONDS,
        SynchronousQueue(), threadFactory("vpn connection pool", true)
    )
    private val calls=ConcurrentHashMap<String,AsyncCall>()

    fun dispatch(pWriter: PacketWriter, packet: Packet) {
        val destIp = packet.ipHeader.destinationAddresses.hostAddress
        var a = calls[destIp]
        if (a ==null){
            a= AsyncCall(pWriter, packet)
            calls[destIp] = a
        }
        executor.execute(a)
    }

    inner class AsyncCall(val pWriter: PacketWriter, val packet: Packet) : Runnable {
        var syncCount = 0
        var packId = 1

        override fun run() {
            when (packet.ipHeader.protocol) {
                Protocol.TCP -> {
                    val tcpHeader = packet.tlHeader as TcpHeader
                    val sourceIp = packet.ipHeader.sourceAddresses.hostAddress
                    val sourcePort = tcpHeader.sourcePort
                    val destIp = packet.ipHeader.destinationAddresses.hostAddress
                    val destPort = tcpHeader.destinationPort
                    val srcAddr = InetSocketAddress(sourceIp, sourcePort)
                    val destAddr = InetSocketAddress(destIp, destPort)
                    val key="${destIp}:${destPort}"
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
                                    srcAddr, destAddr, seqNo, ack, packId
                                )
                            } else {
                                createAckAndRSTPacketOnHandshake(
                                    srcAddr, destAddr, seqNo, ack, packId)
                            }
                            pWriter.writePacket(p)
                            Log.d(TAG, "$key syncCount$syncCount packId:$packId\npacket:$p")
                            ++packId
                        } else {
                            val ack = tcpHeader.sequenceNo + 1
                            Log.d(TAG, "$key syncCount$syncCount packId:$packId\n")
                        }

                        ++syncCount
                    } else if (controlbit.hasACK) {//send data to remote
                        Log.e(
                            TAG,
                            "req buffer remaining:${packet.payload?.remaining()}"
                        )
                        packet.payload?.let {
                            conn?.send(it) { respBuffer ->
                                Log.d(
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
                Protocol.UDP -> {
//                    udpHandler.sendEmptyMessage(1)
                }
                else -> {
                    Log.d(TAG, "not find type :${packet.ipHeader.protocol}")
                }
            }
        }

    }
}

