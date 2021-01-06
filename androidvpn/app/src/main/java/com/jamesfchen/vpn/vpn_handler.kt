package com.jamesfchen.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.jamesfchen.vpn.client.PacketDispatcher
import com.jamesfchen.vpn.protocol.*
import okio.ByteString.Companion.toByteString
import java.net.InetSocketAddress
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
        const val TAG = "${Constants.TAG}/vpn_thread"

    }

    val tcpHandler: TcpHandler
    val udpHandler: UdpHandler
    val dispatcher: PacketDispatcher

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
                       dispatcher.dispatch(pWriter,packet)
                    } else {
//                        Log.d(TAG, "packet is null")
                    }
                    sleep(50)
                }
            }
        }
    }

}
