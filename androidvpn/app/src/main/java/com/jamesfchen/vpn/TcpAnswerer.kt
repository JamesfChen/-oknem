package com.jamesfchen.vpn

import android.os.Handler
import android.os.Message
import android.util.Log
import com.jamesfchen.vpn.Constants.sInterceptIps
import com.jamesfchen.vpn.client.Connection
import com.jamesfchen.vpn.client.ConnectionPool
import com.jamesfchen.vpn.client.PacketDispatcher.Companion.TAG
import com.jamesfchen.vpn.protocol.*
import okio.ByteString.Companion.toByteString
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/08/2021  Fri
 */
class TcpAnswerer(val pWriter: PacketWriter, val answerHandler: Handler,val answerers: ConcurrentHashMap<String, TcpAnswerer>) : Runnable {
    var isusable: Boolean = false
    var syncCount = 0
    var packId = 1
    var seqNo = 1L
    var ackNo = 0L

    val donotConnect = mutableSetOf<String>()
    val connected = mutableSetOf<String>()
    val connectionPool=ConnectionPool
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
            try {
               val conn= if (destPort != 443) {
                val (cc, isUsable) = connectionPool.get(key)
                    cc as Connection
                }else{
                    null
                }
//                printDontConnect(isUsable as Boolean, conn, packet)
                val controlbit = tcpHeader.controlBit
                if (controlbit.hasSYN) {
//                    if (sInterceptIps.contains(packet.ipHeader.destinationAddresses.hostAddress)) {
//                        Log.d(
//                            TAG,
//                            ">>>receive client's syn packet: req buffer ${packet}"
//                        )
//                    }
                    if (syncCount == 0) {
                        ackNo = tcpHeader.sequenceNo + 1
                        if (destPort != 443) {
                            val p = createSynAndAckPacketOnHandshake(
                                destAddr, srcAddr, 1L, ackNo, packId
                            )
                            pWriter.writePacket(p)

                        } else {
                            val p = createAckAndRSTPacketOnHandshake(
                                destAddr, srcAddr, 1L, ackNo, packId
                            )
                            pWriter.writePacket(p)
//                            val obtain = Message.obtain()
//                            obtain.obj = key
//                            answerHandler.sendMessage(obtain)
                            answerers.remove(destIp)
//                            ConnectionPool.remove(destIp)
                        }
                        ++seqNo
                        ++packId

                    } else {
                        ackNo = tcpHeader.sequenceNo + 1
                    }
                    ++syncCount
                } else if (controlbit.hasACK) {//send data to remote
                    val totalBytes = packet.toByteBuffer()
                    val totalRemaing = totalBytes.remaining()
                    if (totalRemaing == 0 || ackNo >= totalRemaing + tcpHeader.sequenceNo) {
                        continue
                    }
                    ackNo = seqNo
                    ackNo += totalRemaing
                    if (sInterceptIps.contains(packet.ipHeader.destinationAddresses.hostAddress)) {
                        Log.d(
                            TAG,
                            ">>>receive client's ack packet: req buffer ${packet}"
                        )
                    }
                    val payloadSize = packet.payload?.size ?: 0
                    if (payloadSize > 0) {
                        conn?.send(packet.payload!!) { respBuffer ->
                            val array = respBuffer.array()
                            val remaining = respBuffer.remaining()
                            Log.e(
                                TAG,
                                "${destIp}:${destPort} resp buffer size:${remaining} ${
                                    respBuffer.toByteString().utf8()
                                }"
                            )
                            val p = createAckPacketOnDataExchange(
                                destAddr,
                                srcAddr,
                                seqNo,
                                ackNo,
                                packId,
                                array
                            )
                            pWriter.writePacket(p)
                            ++packId
                            seqNo += remaining
                        }
                    }
                    //在应答客户端ack时 seqNo=客户端seqNo ,ackNo=客户端seqNo+报文大小
                    pWriter.writePacket(
                        createAckPacketOnHandshake(
                            destAddr, srcAddr, seqNo, ackNo, packId
                        )
                    )
                    ++packId

                } else if (controlbit.hasFIN) {//close connection
                    ackNo = tcpHeader.sequenceNo+1
                    val p = createAckPacketOnWave(destAddr,srcAddr,seqNo,ackNo,packId)
                    pWriter.writePacket(p)
                    ++packId
                    ++seqNo
                } else if (controlbit.hasPSH) {

                } else if (controlbit.hasRST) {

                } else if (controlbit.hasURG) {

                }
            } catch (e: SocketException) {
                Log.e(TAG, "socket error:" + Log.getStackTraceString(e) + "\t" + "src:${sourceIp}:$sourcePort  dest:$key\t")
                val p = createRSTPacketOnHandshake(
                    destAddr,
                    srcAddr,
                    1L,
                    tcpHeader.sequenceNo + 1,
                    packId
                )
                pWriter.writePacket(p)
//                val obtain = Message.obtain()
//                obtain.obj = key
//                answerHandler.sendMessage(obtain)
                answerers.remove(destIp)
//                connectionPool.remove(key)
            } catch (e: ConnectException) {
                Log.e(
                    TAG,
                    "connect error:" + Log.getStackTraceString(e) + "\t" + "src:${sourceIp}:$sourcePort  dest:$key\t"
                )
                //closeRst
                val p = createRSTPacketOnHandshake(
                    destAddr,
                    srcAddr,
                    1L,
                    tcpHeader.sequenceNo + 1,
                    packId
                )
//                pWriter.writePacket(p)
//                val obtain = Message.obtain()
//                obtain.obj = key
//                answerHandler.sendMessage(obtain)
                answerers.remove(destIp)
//                connectionPool.remove(key)
                break
            }
        }
    }

    private fun printDontConnect(isUsable: Boolean, c: Connection, packet: Packet) {
        val tcpHeader = packet.tlHeader as TcpHeader
        val sourceIp = packet.ipHeader.sourceAddresses.hostAddress
        val sourcePort = tcpHeader.sourcePort
        val destIp = packet.ipHeader.destinationAddresses.hostAddress
        val destPort = tcpHeader.destinationPort
        if (destPort != 443) {
            val sb = StringBuilder(
                "vpn source:${sourceIp}:$sourcePort  dest:${destIp}:${destPort}\t" +
                        "isUsable:$isUsable connecting socket local:${c.localAddress} remote:${c.remoteAddress}"
            )
            if (donotConnect.contains(destIp)) {
                sb.append("\n")
                    .append("${destIp}:${destPort} from port 443 down to port 80")
                connected.add(destIp)
            }
//                            Log.d(TAG, sb.toString())
        } else {
            if (!connected.contains(destIp)) {
                Log.i(
                    TAG,
                    "don't connect port 443 dest:${destIp}:${destPort}  packet:${(packet.tlHeader as TcpHeader).controlBit}"
                )
            }
            donotConnect.add(destIp)
        }
    }

    //handshake
    fun onAnswerRstAndAckPacketOnHandshake(packet: Packet) {}
    fun onAnswerSynAndAckPacketOnHandshake(
        packet: Packet,
        tcpHeader: TcpHeader,
        destAddr: InetSocketAddress,
        destPort: Int,
        srcAddr: InetSocketAddress
    ) {
    }

    fun onAnswerAckPacketOnHandshake(packet: Packet) {}

    //data exchange
    fun onAnswerAckPacketOnDataExchange(packet: Packet) {}

    //wave
    fun onAnswerAckPacketOnWave(packet: Packet) {}
    fun onAnswerFinPacketOnWave(packet: Packet) {}
}