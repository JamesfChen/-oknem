package com.jamesfchen.vpn

import android.os.*
import android.util.Log
import com.jamesfchen.vpn.client.PacketDispatcher
import com.jamesfchen.vpn.protocol.*
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


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
        const val TAG = "${Constants.TAG}/dispatcher"
        val executor = ThreadPoolExecutor(
            0, Int.MAX_VALUE, 60L, TimeUnit.SECONDS,
            SynchronousQueue(), threadFactory("vpn connection pool", true)
        )
    }

    val dispatcher: PacketDispatcher

    private val answerers = ConcurrentHashMap<String, TcpAnswerer>()
    val answerHandler:AnswerHandler
    private var mPWriter:PacketWriter?=null
    inner class AnswerHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val key = msg.obj as String
            val (destIp, destPort) = key.split(":")
            answerers.remove(destIp)
//            ConnectionPool.remove(key)
        }
    }

    init {
        dispatcher = PacketDispatcher()
        val aThread = AnswerHandlerThread()
        aThread.start()
        answerHandler=AnswerHandler(aThread.looper)

    }

    override fun run() {
        PacketReader(vpnInterface).use { pReader ->
            PacketWriter(vpnInterface).use { pWriter ->
                mPWriter=pWriter
                while (true) {
                    val packet = pReader.nextPacket()
                    if (packet != null) {
//                        dispatcher.dispatch(pWriter, packet)
                        val destIp = packet.ipHeader.destinationAddresses.hostAddress
                        when (packet.ipHeader.protocol) {
                            Protocol.TCP -> {
                                var answer = answerers[destIp]
                                if (answer == null) {
                                    answer = TcpAnswerer(pWriter,answerHandler,answerers)
                                    answer.isusable = false
                                    answerers[destIp] = answer
                                    executor.execute(answer)
                                } else {
                                    answer.isusable = true
                                }
                                answer.dispatch(packet)
                            }
                            Protocol.UDP -> {
                            }
                            Protocol.ICMP -> {
                            }
                            Protocol.IGMP -> {
                            }
                            Protocol.IP -> {
                            }
                            Protocol.IGRP -> {
                            }
                            Protocol.OSPF -> {
                            }
                            Protocol.IPv6_ICMP -> {
                            }
                            Protocol.MplsInIp -> {
                            }
                            else -> {
                                Log.d(TAG, "not find type :${packet.ipHeader.protocol}")
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

class AnswerHandlerThread() : HandlerThread("answer_event_thread") {

}
class TcpHandlerThread() : HandlerThread("tcp_event_thread") {

}

class TcpHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
//        Log.d(T_TAG, "tcp message")
//        val packet = msg.obj as Packet
//        val destIp = packet.ipHeader.destinationAddresses.hostAddress
//        val destPort = (packet.tlHeader as TcpHeader).destinationPort
//        Log.d(T_TAG, "remote connect:${destIp}:${destPort}")

    }
}


class UdpHandlerThread() : HandlerThread("udp_event_thread") {

}

class UdpHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
//        Log.d(U_TAG, "udp message")
    }

}


class PacketReader constructor(vpnInterface: ParcelFileDescriptor) : AutoCloseable, Closeable {
    private val buffer = ByteBuffer.allocate(16384)
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


