package com.jamesfchen.vpn.client

import android.util.Log
import com.jamesfchen.vpn.VpnHandlerThread
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Copyright ® $ 2021
 * All right reserved.
 *
 * @since: Jan/13/2021  Wed
 *
 */

//Packet(ipHeader="ipheader":{"version":V4,"ihl":5,"headerLen":20,"typeOfService":{"precedenceType":ROUTINE,"d":0,"t":0,"r":0},"totalLength":60,"identification":26741,"flags":{"df":0,"mf":0},"fragmentOffset":0,"ttl":26,"protocol":UDP,"headerChecksum":17453,"sourceAddresses":/10.0.0.2,"destinationAddresses":/115.236.118.33}, tlHeader=UdpHeader(sourcePort=34160, destPort=53, udpLen=40),payload:32)
class BioUdpConnection(
    val ip:String,val port:Int,
    val socket: DatagramSocket
) : Connection() {
    override fun send(reqBuffer: ByteArray, block: (respBuffer: ByteBuffer) -> Unit) {
        socket.send(DatagramPacket(reqBuffer, reqBuffer.size, InetSocketAddress(ip,port)))

        VpnHandlerThread.executor.submit {
            synchronized(this) {
                while (true) {
                    val ba = ByteArray(16 * 1024)
                    socket.receive(DatagramPacket(ba, 1024))
                    val size = socket.receiveBufferSize
                    val wrap = ByteBuffer.wrap(ba)
                    Log.d("udp","size:$size")
                    wrap.limit(size)
                    block(wrap)
                }
            }
        }
    }

    override val remoteAddress: InetSocketAddress? = socket.remoteSocketAddress as InetSocketAddress?
    override val localAddress: InetSocketAddress? = socket.localSocketAddress as InetSocketAddress?
    override fun close() {

    }

}

class AioUdpConnection(
) : Connection() {
    override fun send(reqBuffer: ByteArray, block: (respBuffer: ByteBuffer) -> Unit) {

    }

    override val remoteAddress: InetSocketAddress? = null
    override val localAddress: InetSocketAddress? = null
    override fun close() {

    }

}

