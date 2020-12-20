package com.jamesfchen.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.jamesfchen.vpn.protocol.TcpHandler
import com.jamesfchen.vpn.protocol.TcpHandlerThread
import com.jamesfchen.vpn.protocol.UdpHandler
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


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
class VpnHandlerThread(vpnInterface: ParcelFileDescriptor) : Thread("vpn_thread") {
    companion object {
        const val TAG = "${Constants.TAG}/vpn_thread"
    }

    val tcpHandler: TcpHandler
    val udpHandler: UdpHandler
    val vpnOutput: FileChannel = FileOutputStream(vpnInterface.fileDescriptor).channel
    val vpnInput: FileChannel = FileInputStream(vpnInterface.fileDescriptor).channel

    init {
        val tcpThread = TcpHandlerThread()
        tcpThread.start()
        val udpThread = TcpHandlerThread()
        udpThread.start()

        tcpHandler = TcpHandler(tcpThread.looper)
        udpHandler = UdpHandler(udpThread.looper)

    }

    override fun run() {
        PacketReader(vpnInput).use { pReader ->
            PacketWriter(vpnOutput).use { pWriter ->

                var packet = pReader.nextPacket()
                while (packet != null) {
//                when (ipPacket.header.type) {
//                    PacketType.TCP -> tcpHandler.sendEmptyMessage(1)
//                    PacketType.UDP -> udpHandler.sendEmptyMessage(1)
//                    else -> Log.d(TAG, "not find type")
//                }
                    for (i in 0..9) {
                        val myClient = AioSocketClient()
                        myClient.connect("323", 12)
//                    myClient.write("aaaaaaaaaaaaaaaaaaaaaa")
                        myClient.send(ByteBuffer.wrap("aaaaaaaaaaaaaaaaaaaaaa".toByteArray())) { respBuffer ->
                            Log.d(TAG, "buffer size:${respBuffer.remaining()}")
                            val header = Header(PacketType.TCP)
                            pWriter.writePacket(Packet(header, respBuffer))
                        }

//                        val client = OkHttpClient()
//                        val listener = MyWebSocketListener()
//                        val request: Request = Request.Builder()
//                            .url("ws://echo.websocket.org")
//                            .build()
//                        val webSocket = client.newWebSocket(request, listener)
//                        webSocket.send("safasf")

                    }
                    packet = pReader.nextPacket()
                }
            }
        }
    }

}
