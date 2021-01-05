package com.jamesfchen.vpn.protocol

import com.jamesfchen.vpn.*
import java.lang.reflect.Array.getByte
import java.nio.ByteBuffer

/**
 * Copyright ® $ 2021
 * All right reserved.
 * ICMP协议是一个网络层协议。
一个新搭建好的网络，往往需要先进行一个简单的测试，来验证网络是否畅通；但是IP协议并不提供可靠传输。如果丢包了，IP协议并不能通知传输层是否丢包以及丢包的原因。
所以我们就需要一种协议来完成这样的功能–ICMP协议

ping的实现基于ICMP协议

[icmp rfc](https://tools.ietf.org/html/rfc792)
 [Linux编程之PING的实现](https://www.cnblogs.com/skyfsm/p/6348040.html)
 */
data class IcmpHeader(
    val type: Int, val code: Code,
    val checksum: Int
) {
    /*

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             unused                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Internet Header + 64 bits of Original Data Datagram      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

}

fun IcmpHeader.toByteBuffer(): ByteBuffer {
    val buffer = ByteBuffer.allocate(type)
    buffer.putUByte(type.toByte())
    buffer.putUByte(code.ordinal.toByte())
    buffer.putUShort(checksum)
    buffer.flip()
    return buffer
}
fun ByteBuffer.getIcmpHeader(): IcmpHeader {
    return IcmpHeader(getUByte(),Code.parseCode(getUByte()),getUShort())
}

enum class Code {
    /*
       Code

      0 = net unreachable;

      1 = host unreachable;

      2 = protocol unreachable;

      3 = port unreachable;

      4 = fragmentation needed and DF set;

      5 = source route failed.
     */
    NET_UNREACHABLE,
    HOST_UNREACHABLE,
    PROTOCOL_UNREACHABLE,
    PORT_UNREACHABLE,
    FRAGMENTATION_NEEDED_AND_DF_SET,
    SOURCE_ROUTE_FAILED,
    UNKNOW;
    companion object {
        fun parseCode(v: Int): Code {
            for (e in values()) {
                if (e.ordinal == v) {
                    return e
                }
            }
            return UNKNOW
        }
    }
}