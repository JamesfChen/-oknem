package com.jamesfchen.vpn

import com.jamesfchen.vpn.protocol.IpHeader
import com.jamesfchen.vpn.protocol.Protocol
import com.jamesfchen.vpn.protocol.TcpHeader
import com.jamesfchen.vpn.protocol.UdpHeader
import okhttp3.internal.and
import java.nio.ByteBuffer

/**
 * Copyright ® $ 2020
 * All right reserved.
 *
 * java的byte是有符号数据，而python的byte无符号数据,有符号的byte数据存储范围-128~127.所以如果byte要存入区间之外的数据，
 * 比如存入255(0xff),就要被转换成负数；python存入什么值就是什么值。
 *
 */
private fun uByte(nm: String): Int {
    return try {
        //0xfc
        Integer.decode(nm) and 0xff
    } catch (e: NumberFormatException) {
        try {
            //fc
            Integer.parseInt(nm, 16) and 0xff
        } catch (e: NumberFormatException) {
            return -1
        }
    }
}

fun ByteBuffer.getUByte(): Int {
    return uByte(get())
}

fun ByteBuffer.putUByte(v: Byte) = apply {
    put(v)
}

private fun uByte(b: Byte): Int {
    return b and 0xff
}

fun ByteBuffer.getBytes(size: Int): ByteArray {
    val bs = ByteArray(size)
    for (i in 0 until size) {
        bs[i] = get()
    }
    return bs
}

fun ByteBuffer.getUShort(): Int {
    return uShort(short)
}

fun ByteBuffer.putUShort(v: Int) = apply {
    put((v and 0xff00 shr 8).toByte())
    put((v and 0x00ff).toByte())
}

//无符号16位
private fun uShort(s: Short): Int {
    return s and 0xff_ff
}

//无符号32位
fun ByteBuffer.getUInt(): Long {
    return uInt(int)
}

fun ByteBuffer.putUInt(v: Long) = apply {
    put((v and 0xff00_0000 shr 24).toByte())
    put((v and 0x00ff_0000 shr 16).toByte())
    put((v and 0x0000_ff00 shr 8).toByte())
    put((v and 0x0000_00ff).toByte())
}

//无符号32位
private fun uInt(n: Int): Long {
    return n and 0xff_ff_ff_ff
}

//fun uLong(l: Long): Long {
//    return l and 0xff_ff_ff_ff_ff_ff_ff_ff
//}

