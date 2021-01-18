package com.jamesfchen.vpn.protocol

import android.util.Log
import com.jamesfchen.vpn.Constants
import com.jamesfchen.vpn.Constants.TAG
import com.jamesfchen.vpn.getUByte
import com.jamesfchen.vpn.getUShort
import java.nio.ByteBuffer

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/20/2020  Sun
 *
 *

layer | devices|传输数据的基本单位
---|---|---
应用层|应用网关|
传输层|传输网关|数据段、报文段
网络层|路由器|数据分组
数据链路层|网桥、交换机|数据帧
物理层|中继器、集线器|比特流
Protocol Layering

+---------------------+
|     higher-level    |
+---------------------+
|        TCP          |
+---------------------+
|  internet protocol  |
+---------------------+
|communication network|
+---------------------+

一个ip包的payload size最大为1480(tcp报文段的payload size最大为1460 或者 udp报文段payload size为1472)，
当应用层的1460(1472)< payload size<= 0xffff(64k)时，路由器就会将其切割成许多小块，进行分组发送。
同一款分组包中的Identification相同，每个分组包的Fragment Offset表示其在完整包中的偏移位置，借此来组装成完整的包
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| ip header | transport layer header| payload(http/ftp 1)     |-->第1个Packet
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| ip header |                          payload(http/ftp 2)    |-->第2个Packet
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| ip header |                   ...                           |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| ip header |                         payload(http/ftp n-1)   |-->第n-1个Packet
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| ip header |                         payload(http/ftp n)     |-->第n个Packet
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

 */
//MTU(Maximum Transmission Unit)
const val P_TAG = "${Constants.TAG}/packet"
const val BUFFER_SIZE = 16*1024//16384
// 4*1024
interface TransportLayerHeader {
    fun toByteBuffer(): ByteBuffer
}

data class Packet(
    val ipHeader: IpHeader,
    var tlHeader: TransportLayerHeader? = null,
    val payload: ByteArray? = null
) {
    override fun toString(): String {
        return "Packet(ipHeader=$ipHeader, tlHeader=$tlHeader,payload:${payload?.size})"
    }

    fun toByteBuffer(): ByteBuffer {
        val ipHeaderBuffer = ipHeader.toByteBuffer()
        val tlHeaderBuffer = tlHeader?.toByteBuffer()
        val totalBuffer = ByteBuffer.allocate(ipHeader.totalLength)
        totalBuffer.put(ipHeaderBuffer)
        tlHeaderBuffer?.let {
            totalBuffer.put(it)
        }
        payload?.let {
            totalBuffer.put(ByteBuffer.wrap(it))
        }
        totalBuffer.flip()
        return totalBuffer
    }

    /** 对于报文的发送者(最后计算得到的checksum为一个有效值)
     * 1. 将checksum设置为0
     * 2. 将pesudo header 的sum + tcp header sum + tcp payload sum
     * 3. 取sum反码
     *
     * 对于报文的接受者(最后计算的checksum为0)
     * 1. 将pesudo header 的sum + tcp header sum + tcp payload sum
     * 2. 取sum反码
     *
     */
    fun computeTcpChecksum(): Int {
        //pseudoHeaderSum
        var sum = 0
        var len =  (tlHeader as TcpHeader).headerLen + (payload?.size ?: 0)
        // Calculate pseudo-header checksum
        var buffer = ByteBuffer.wrap(ipHeader.sourceAddresses.address)
        sum += buffer.getUShort() + buffer.getUShort()
        buffer = ByteBuffer.wrap(ipHeader.destinationAddresses.address)
        sum += buffer.getUShort() + buffer.getUShort()
        sum +=  Protocol.TCP.typeValue+ len
        // Calculate TCP segment checksum
        val b = toByteBuffer()
        b.position(IP4_HEADER_SIZE)
        while (len > 1) {
            val e = b.getUShort()
            sum += e
            len -= 2
        }
        if (len > 0) sum += b.getUByte() shl 8
        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        sum = 0xff_ff - sum//取反   sum.inv()取反有问题
        return sum
    }
}

fun ByteBuffer.getPacket(): Packet {
    val ipHeader = getIpHeader()
    var tlHeader: TransportLayerHeader? = null
    var tlLen = 0
    when (ipHeader.protocol) {
        Protocol.TCP -> {
            tlHeader = getTcpHeader()
            tlLen = (tlHeader as? TcpHeader)?.headerLen ?: 0
        }
        Protocol.UDP -> {
            tlHeader = getUdpHeader()
            tlLen = UDP_HEADER_SIZE
        }
        else -> {
        }
    }

    val p = Packet(ipHeader, tlHeader, this.array().copyOfRange(ipHeader.headerLen + tlLen,ipHeader.totalLength))

    if (ipHeader.protocol == Protocol.TCP){
        val computeChecksum = p.computeTcpChecksum()
//        Log.d(TAG,"tcp computeChecksum:$computeChecksum")
    }
//    if (computeChecksum == 0) {//从流中读取到一个packet，作为接受者，计算checksum应该为0
//
//    }
    return p
}