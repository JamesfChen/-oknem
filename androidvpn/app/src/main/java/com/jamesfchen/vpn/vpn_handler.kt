package com.jamesfchen.vpn

import android.os.*
import android.util.Log
import com.jamesfchen.vpn.client.PacketDispatcher
import com.jamesfchen.vpn.client.TcpTunnel
import com.jamesfchen.vpn.protocol.*
import okio.ByteString.Companion.toByteString
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
    }

    val tcpHandler: TcpHandler
    val udpHandler: UdpHandler
    val dispatcher: PacketDispatcher
    private val executor = ThreadPoolExecutor(
        0, Int.MAX_VALUE, 60L, TimeUnit.SECONDS,
        SynchronousQueue(), threadFactory("vpn connection pool", true)
    )
    private val tcpTunnels = ConcurrentHashMap<String, TcpTunnel>()
    init {
        val tcpThread = TcpHandlerThread()
        tcpThread.start()
        val udpThread = TcpHandlerThread()
        udpThread.start()

        tcpHandler = TcpHandler(tcpThread.looper)
        udpHandler = UdpHandler(udpThread.looper)
        dispatcher = PacketDispatcher()

    }

    override fun run() {
        PacketReader(vpnInterface).use { pReader ->
            PacketWriter(vpnInterface).use { pWriter ->
                while (true) {
                    val packet = pReader.nextPacket()
                    if (packet != null) {
                        when (packet.ipHeader.protocol) {
                            Protocol.TCP -> {
                                val msg = Message.obtain()
                                msg.obj = packet
                                tcpHandler.sendMessage(msg)
                            }
                            Protocol.UDP -> {
                                udpHandler.sendEmptyMessage(1)
                            }
                            else -> {
//                                Log.d(TAG, "not find type :${packet.ipHeader.protocol}")
                            }
                        }

//                        dispatcher.dispatch(pWriter, packet)

                        val destIp = packet.ipHeader.destinationAddresses.hostAddress
                        when (packet.ipHeader.protocol) {
                            Protocol.TCP -> {
                                var tunnel = tcpTunnels[destIp]
                                if (tunnel == null) {
                                    tunnel = TcpTunnel(pWriter)
                                    tunnel.isusable= false
                                    tcpTunnels[destIp] = tunnel
                                    executor.execute(tunnel)
                                }else{
                                    tunnel.isusable= true
                                }
//                                Log.d(TAG, ">>>$destIp tcpTunnel :isusable ${tunnel.isusable} tcpTunnels totalsize:${tcpTunnels.size}")
                                tunnel.dispatch(packet)
                            }
                            Protocol.UDP -> { }
                            Protocol.ICMP -> { }
                            Protocol.IGMP -> { }
                            Protocol.IP -> { }
                            Protocol.IGRP -> { }
                            Protocol.OSPF -> { }
                            Protocol.IPv6_ICMP -> { }
                            Protocol.MplsInIp -> { }
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

