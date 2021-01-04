package com.jamesfchen.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.jamesfchen.vpn.protocol.*
import okio.ByteString.Companion.toByteString
import kotlin.random.Random.Default.Companion


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
                                val destIp = packet.ipHeader.destinationAddresses.hostAddress
                                val destPort = (tcpHeader).destinationPort
                                Log.d(TAG, "dest:${destIp}:${destPort}")
                                val controlbit = tcpHeader.controlBit
                                if (controlbit.hasSYN) {
//                                    pWriter.writeAckPacket()
                                } else if (controlbit.hasACK) {
//                                    val myClient =
//                                        Client.createAndConnect(destIp, destPort, aioSocket = true)
//                                    Log.d(
//                                        TAG,
//                                        "socket remote:${myClient.remoteAddress} local:${myClient.localAddress}"
//                                    )
//                                    Log.d(TAG, "req buffer remaining:${packet.buffer.remaining()}")
//                                    myClient.send(packet.buffer) { respBuffer ->
//                                        Log.d(
//                                            TAG,
//                                            "resp buffer size:${respBuffer.remaining()} ${
//                                                respBuffer.toByteString().utf8()
//                                            }"
//                                        )
////                                        pWriter.writePacket()
//                                    }
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
