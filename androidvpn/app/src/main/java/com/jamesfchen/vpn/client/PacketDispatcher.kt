package com.jamesfchen.vpn.client

import android.util.Log
import com.jamesfchen.vpn.TcpAnswerer
import com.jamesfchen.vpn.Constants
import com.jamesfchen.vpn.PacketWriter
import com.jamesfchen.vpn.protocol.*
import com.jamesfchen.vpn.threadFactory
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

    private val executor = ThreadPoolExecutor(
        0, Int.MAX_VALUE, 60L, TimeUnit.SECONDS,
        SynchronousQueue(), threadFactory("vpn connection pool", true)
    )
    private val tunnels = ConcurrentHashMap<String, TcpAnswerer>()

    fun dispatch(pWriter: PacketWriter, packet: Packet) {
        val destIp = packet.ipHeader.destinationAddresses.hostAddress
        when (packet.ipHeader.protocol) {
            Protocol.TCP -> {
                var a = tunnels[destIp]
                if (a == null) {
//                    a = Answerer(pWriter)
//                    tunnels[destIp] = a
//                    executor.execute(a)
                }
//                a.dispatch(packet)
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

    }
}

