package com.jamesfchen.vpn

import android.os.Handler
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
import java.util.concurrent.ExecutionException

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/08/2021  Fri
 *
 * [两张动图-彻底明白TCP的三次握手与四次挥手](https://blog.csdn.net/qzcsu/article/details/72861891)
 */
class TcpAnswerer(
    val pWriter: PacketWriter,
    val answerHandler: Handler,
    val answerers: ConcurrentHashMap<String, TcpAnswerer>
) : Runnable {
    var isusable: Boolean = false
    var syncCount = 0
    var packId = 1
    private var answererSeqNo = 1L//sequence用来标识哪一个回答者的tcp报文
    private var answererAckNo = 0L//acknowledgement用来标识响应的是哪一个调用者的tcp报文
    private var status: TcpStatus = TcpStatus.LISTEN

    val donotConnect = mutableSetOf<String>()
    val connected = mutableSetOf<String>()
    val connectionPool = ConnectionPool
    private val packetQueue = ArrayBlockingQueue<Packet>(100)
    fun dispatch(packet: Packet) {
        packetQueue.offer(packet)
    }

    private fun closeConnection(destIp: String, destPort: Int) {
        answerers.remove(destIp)
        connectionPool.remove("${destIp}:${destPort}")
        //val obtain = Message.obtain()
//                obtain.obj = key
//                answerHandler.sendMessage(obtain)

    }

    override fun run() {
        status = TcpStatus.LISTEN
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
                val (cc, isUsable) = connectionPool.get(key)
                val conn = cc as Connection
                printDontConnect(isUsable as Boolean, conn, packet)
                val controlbit = tcpHeader.controlBit
                if (controlbit.hasSYN) {
                    if (syncCount == 0) {
                        answererAckNo = tcpHeader.sequenceNo + 1
                        if (destPort != 443) {
                            val p = createSynAndAckPacketOnHandshake(
                                destAddr, srcAddr, 1L, answererAckNo, packId
                            )
                            pWriter.writePacket(p)

                        } else {
                            val p = createAckAndRSTPacketOnHandshake(
                                destAddr, srcAddr, 1L, answererAckNo, packId
                            )
                            pWriter.writePacket(p)
//                            val obtain = Message.obtain()
//                            obtain.obj = key
//                            answerHandler.sendMessage(obtain)
                            answerers.remove(destIp)
                        }
                        ++answererSeqNo
                        ++packId

                    } else {
                        answererAckNo = tcpHeader.sequenceNo + 1
                    }
                    ++syncCount
                    status = TcpStatus.SYN_RECEIVED
                } else if (controlbit.hasACK) {
                    if (status == TcpStatus.LAST_ACK) {
                        status = TcpStatus.CLOSED
                        if (sInterceptIps.contains(packet.ipHeader.destinationAddresses.hostAddress)) {
                            Log.d(
                                TAG,
                                ">>>receive client's last ack packet: ${packet}"
                            )
                        }
                        //todo:close stream
                        val p = createFinAndAckPacketOnWave(
                            destAddr,
                            srcAddr,
                            answererSeqNo,
                            answererAckNo,
                            packId
                        )
                        pWriter.writePacket(p)
                        ++packId
                        closeConnection(destIp, destPort)
                        return
                    }
                    val askerSeqNo = tcpHeader.sequenceNo
                    val payloadSize = packet.payload?.size ?: 0
                    if (payloadSize == 0 || answererAckNo >= payloadSize + askerSeqNo) {
                        //握手最后一个包
                        status = TcpStatus.ESTABLISHED
                        continue
                    }
                    //send data to remote
                    answererAckNo = askerSeqNo
                    answererAckNo += payloadSize
                    if (sInterceptIps.contains(packet.ipHeader.destinationAddresses.hostAddress)) {
                        Log.d(
                            TAG,
                            ">>>receive client's ack packet: req buffer ${packet}"
                        )
                    }

                    conn?.send(packet.payload!!) { respBuffer ->
                        val remaining = respBuffer.remaining()
                        val array = ByteArray(remaining)
                        respBuffer.get(array)
                        Log.e(
                            TAG,
                            "${destIp}:${destPort} resp buffer size:${remaining} - ${array.size}\n" +
                                    respBuffer.toByteString().utf8()
                        )
                        val unitSize: Int = BUFFER_SIZE - IP4_HEADER_SIZE - TCP_HEADER_SIZE
                        var offset = 0
                        while (offset < array.size) {
                            val len =
                                if (offset + unitSize > array.size) array.size - offset else unitSize
                            val unit = ByteArray(len)
                            System.arraycopy(array, offset, unit, 0, len)
                            val p = createAckPacketOnDataExchange(
                                destAddr,
                                srcAddr,
                                answererSeqNo,
                                answererAckNo,
                                packId,
                                array
                            )
                            pWriter.writePacket(p)
                            answererSeqNo += remaining
                            offset += len
                        }
                    }
                    //在应答客户端ack时 seqNo=客户端seqNo ,ackNo=客户端seqNo+报文大小
                    //todo: 在握手期间应答客户端不会发送只带ack控制符的包，这里有点问题？
                    pWriter.writePacket(
                        createAckPacketOnHandshake(
                            destAddr, srcAddr, answererSeqNo, answererAckNo, packId
                        )
                    )
                    ++packId


                } else if (controlbit.hasFIN) {//close connection
                    if (sInterceptIps.contains(packet.ipHeader.destinationAddresses.hostAddress)) {
                        Log.d(TAG, ">>>receive client's fin packet: ${packet}")
                    }
                    answererAckNo = tcpHeader.sequenceNo + 1
                    val p = createAckPacketOnWave(
                        destAddr,
                        srcAddr,
                        answererSeqNo,
                        answererAckNo,
                        packId
                    )
                    pWriter.writePacket(p)
                    ++packId
                    ++answererSeqNo
                    status = TcpStatus.CLOSE_WAIT
                } else if (controlbit.hasPSH) {

                } else if (controlbit.hasRST) {

                } else if (controlbit.hasURG) {

                }
            } catch (e: SocketException) {
                Log.e(
                    TAG,
                    "socket error:" + Log.getStackTraceString(e) + "\t" + "src:${sourceIp}:$sourcePort  dest:$key\t"
                )
                val p = createRSTPacketOnHandshake(
                    destAddr,
                    srcAddr,
                    1L,
                    tcpHeader.sequenceNo + 1,
                    packId
                )
                pWriter.writePacket(p)
                ++packId

                //todo:close stream
                closeConnection(destIp, destPort)
                return
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
                pWriter.writePacket(p)
                ++packId

                //todo:close stream
                closeConnection(destIp, destPort)
                return
            } catch (e: ExecutionException) {
                Log.e(
                    TAG,
                    "1 connect fin: ${e.message} " + "src:${sourceIp}:$sourcePort  dest:$key\t"
                )
                if ("com.jamesfchen.vpn.protocol.TcpFinException" == e.message) {
                    Log.e(TAG, "last ack")
                    //todo:fin close stream
                    status = TcpStatus.LAST_ACK
                } else {
                    Log.e(TAG, "tcp close stream")
                    status = TcpStatus.CLOSED
                    val p = createRstPacketOnWave(
                        destAddr,
                        srcAddr,
                        answererSeqNo,
                        answererAckNo,
                        packId
                    )
                    pWriter.writePacket(p)
                    ++packId
                    closeConnection(destIp, destPort)
                }

            } catch (e: TcpFinException) {
                Log.e(TAG, "2 connect fin:" + "src:${sourceIp}:$sourcePort  dest:$key\t")
                status = TcpStatus.LAST_ACK
                //todo:fin close stream
                return
            } catch (e: TcpRstException) {
                Log.e(TAG, "connect rst:" + "src:${sourceIp}:$sourcePort  dest:$key\t")
                status = TcpStatus.CLOSED
                //todo:rst close stream
                val p = createRstPacketOnWave(
                    destAddr,
                    srcAddr,
                    answererSeqNo,
                    answererAckNo,
                    packId
                )
                pWriter.writePacket(p)
                ++packId
                closeConnection(destIp, destPort)
                return
            }
        }
    }

    var isFirstSyncACK = true
    var isFirstRSTACK = true
    var isFirstACK = true
    var isFirstRST = true
    private fun printDontConnect(isUsable: Boolean, c: Connection, packet: Packet) {
        val ipHeader = packet.ipHeader
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
//            Log.d(TAG, sb.toString())
        } else {
            if (!connected.contains(destIp)) {
//                Log.i(TAG, "don't connect port 443 dest:${destIp}:${destPort}  packet:${(packet.tlHeader as TcpHeader).controlBit}")
            }
            donotConnect.add(destIp)
        }
        val controlbit = tcpHeader.controlBit
        if (sInterceptIps.contains(ipHeader.sourceAddresses.hostAddress)) {
            if (isFirstSyncACK && controlbit.hasSYN && controlbit.hasACK) {
                Log.d(TAG, "<<< send  server's sync_ack packet$packet")
                isFirstSyncACK = false
            } else if (isFirstRSTACK && controlbit.hasRST && controlbit.hasACK) {
                isFirstRSTACK = false
                Log.d(TAG, "<<< send  server's rst_ack packet$packet")
            } else if (isFirstACK && controlbit.hasACK) {
                isFirstACK = false
                Log.d(TAG, "<<< send  server's ack packet$packet")
            } else if (isFirstRST && controlbit.hasRST) {
                isFirstRST = false
                Log.d(TAG, "<<< send  server's rst packet$packet")
            }
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