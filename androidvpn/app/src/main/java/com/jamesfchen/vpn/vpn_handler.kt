package com.jamesfchen.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.jamesfchen.vpn.protocol.*
import okio.ByteString.Companion.toByteString
import java.net.InetSocketAddress


/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/19/2020  Sat
 *
 * app --->
 *          vpn --->
 *                   server
 *          vpn <---
 * app <---
 */
class VpnHandlerThread(val vpnInterface: ParcelFileDescriptor) : Thread("vpn_thread") {
    companion object {
        const val TAG = "${Constants.TAG}/vpn_thread"
    }

    val tcpHandler: TcpHandler
    val udpHandler: UdpHandler
    val pool: ConnectionPool = ConnectionPool()

    init {
        val tcpThread = TcpHandlerThread()
        tcpThread.start()
        val udpThread = TcpHandlerThread()
        udpThread.start()

        tcpHandler = TcpHandler(tcpThread.looper)
        udpHandler = UdpHandler(udpThread.looper)

    }

    override fun run() {
        PacketReader(vpnInterface).use { pReader ->
            PacketWriter(vpnInterface).use { pWriter ->
                var syncCount = 0
                var packId = 1
                while (true) {
                    val packet = pReader.nextPacket()
                    if (packet != null) {
                        when (packet.ipHeader.protocol) {
                            Protocol.TCP -> {
//                                val msg = Message.obtain()
//                                msg.obj = packet
//                                tcpHandler.sendMessage(msg)
//                                val packet = msg.obj as Packet
                                val tcpHeader = packet.tlHeader as TcpHeader
                                val sourceIp = packet.ipHeader.sourceAddresses.hostAddress
                                val sourcePort = tcpHeader.sourcePort
                                val destIp = packet.ipHeader.destinationAddresses.hostAddress
                                val destPort = tcpHeader.destinationPort
                                Log.d(
                                    TAG,
                                    "source:${sourceIp}:$sourcePort  dest:${destIp}:${destPort}"
                                )
                                val conn =
                                    if (destPort != 443) pool.get("$destIp:$destPort") else null
                                val controlbit = tcpHeader.controlBit
                                if (controlbit.hasSYN) {
                                    if (syncCount == 0) {
                                        val seqNo = 1L
                                        val ack = tcpHeader.sequenceNo + 1
                                        val srcAddr = InetSocketAddress(sourceIp, sourcePort),
                                        val destAddr = InetSocketAddress(destIp, destPort),
                                        val p = if (destPort != 443) {
                                            createSynAndAckPacketOnHandshake(
                                                srcAddr, destAddr, seqNo, ack, packId
                                            )
                                        } else {
                                            createAckAndRSTPacketOnHandshake(
                                                srcAddr, destAddr, seqNo, ack, packId
                                            )
                                        }
                                        pWriter.writePacket(p)
                                        ++packId
                                    } else {
                                        val ack = tcpHeader.sequenceNo + 1
                                    }
                                    ++syncCount
                                } else if (controlbit.hasACK) {//send data to remote
                                    Log.d(
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
//                                        pWriter.writePacket()
                                        }
                                    }
                                } else if (controlbit.hasFIN) {

                                } else if (controlbit.hasPSH) {

                                } else if (controlbit.hasRST) {

                                } else if (controlbit.hasURG) {

                                }

                            }
                            Protocol.UDP -> {
                                udpHandler.sendEmptyMessage(1)
                            }
                            else -> {
                                Log.d(TAG, "not find type")
                            }
                        }

                    } else {
//                        Log.d(TAG, "packet is null")
                    }
                    sleep(50)
                }
            }
        }
    }

}
