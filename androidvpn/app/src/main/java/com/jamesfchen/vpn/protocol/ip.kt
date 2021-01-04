package com.jamesfchen.vpn.protocol

import android.nfc.Tag
import android.util.Log
import com.jamesfchen.vpn.*
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/19/2020  Sat
 * [IP报文格式](http://www.023wg.com/message/message/cd_feature_ip_message_format.html)
 * [IP协议头部](https://zhangbinalan.gitbooks.io/protocol/content/ipxie_yi_tou_bu.html)
 * [TCP 协议简介](http://www.ruanyifeng.com/blog/2017/06/tcp-protocol.html#:~:text=%E4%BB%A5%E5%A4%AA%E7%BD%91%E6%95%B0%E6%8D%AE%E5%8C%85%EF%BC%88packet,%E6%9C%80%E5%A4%9A%E4%B8%BA1480%E5%AD%97%E8%8A%82%E3%80%82)
 * [rfc791](https://tools.ietf.org/html/rfc791)
 *
 */
const val IP4_HEADER_SIZE = 20
const val IP4_OPTION_HEADER_SIZE = 40
const val IP4_HEADER_MAX_SIZE = 60
const val IP4_PAYLOAD_MAX_SIZE = 1480
const val IP4_SIZE = 1500

//3bit
enum class PrecedenceType(val typeValue: Int) {
    /*
            0     1     2     3     4     5     6     7
      +-----+-----+-----+-----+-----+-----+-----+-----+
      |                 |     |     |     |     |     |
      |   PRECEDENCE    |  D  |  T  |  R  |  0  |  0  |
      |                 |     |     |     |     |     |
      +-----+-----+-----+-----+-----+-----+-----+-----+
     */
    UNKOWN(-1),
    ROUTINE(0b000),//普通
    PRIORITY(0b001),//优先的
    IMMEDIATE(0b010),//立即的发送
    FLASH(0b011),//闪电式的
    FLASH_OVERRIDE(0b100),//比闪电还闪电式的
    CRITIC_ECP(0b101),//CRITIC/ECP
    INTERNETWORK_CONTROL(0b110),//网间控制
    NETWORK_CONTROL(0b111);//网络控制

    companion object {
        fun parseType(v: Int): PrecedenceType {
            for (e in values()) {
                if (e.typeValue == v) {
                    return e
                }
            }
            return UNKOWN
        }
    }
}


data class TypeOfService(val byteValue: Int) {
    companion object {
        //D 时延: 0:普通 1:延迟尽量小
        val normal_delay = 0
        val low_delay = 1

        //T 吞吐量: 0:普通 1:流量尽量大
        val normal_throughput = 0
        val high_throughput = 1

        //R 可靠性: 0:普通 1:可靠性尽量大
        val normal_relibility = 0
        val high_relibility = 1
    }

    val precedenceType: PrecedenceType = PrecedenceType.parseType(byteValue shr 5)
    val d: Int = byteValue and 0b00010000
    val t: Int = byteValue and 0b00001000
    val r: Int = byteValue and 0b00000100
    override fun toString(): String {
        return "{\"precedenceType\":$precedenceType,\"d\":$d,\"t\":$t,\"r\":$r}"
    }

}

data class IpFlag(val byteValue: Int) {
    //    val reserved: Int //1bit
    val df: Int = byteValue shr 1 and 0b01  //1bit Don't Fragment 能否分片位，0表示可以分片，1表示不能分片。
    val mf: Int = byteValue and 0b001 //1bit More Fragment 表示是否该报文为最后一片，0表示最后一片，1代表后面还有。
    override fun toString(): String {
        return "{\"df\":$df,\"mf\":$mf}"
    }
}

enum class Protocol(val typeValue: Int) {
    /*
    常见值：
    0: 保留Reserved
    1: ICMP, Internet Control Message [RFC792]
    2: IGMP, Internet Group Management [RFC1112]
    3: GGP, Gateway-to-Gateway [RFC823]
    4: IP in IP (encapsulation) [RFC2003]
    6: TCP Transmission Control Protocol [RFC793]
    17: UDP User Datagram Protocol [RFC768]
    20: HMP Host Monitoring Protocol [RFC 869]
    27: RDP Reliable Data Protocol [ RFC908 ]
    46: RSVP (Reservation Protocol)
    47: GRE (General Routing Encapsulation)
    50: ESP Encap Security Payload [RFC2406]
    51: AH (Authentication Header) [RFC2402]
    54: NARP (NBMA Address Resolution Protocol) [RFC1735]
    58: IPv6-ICMP (ICMP for IPv6) [RFC1883]
    59: IPv6-NoNxt (No Next Header for IPv6) [RFC1883]
    60: IPv6-Opts (Destination Options for IPv6) [RFC1883]
    89: OSPF (OSPF Version 2) [RFC 1583]
    112: VRRP (Virtual Router Redundancy Protocol) [RFC3768]
    115: L2TP (Layer Two Tunneling Protocol)
    124: ISIS over IPv4
    126: CRTP (Combat Radio Transport Protocol)
    127: CRUDP (Combat Radio User Protocol)
    132: SCTP (Stream Control Transmission Protocol)
    136: UDPLite [RFC 3828]
    137: MPLS-in-IP [RFC 4023]
     */
    UNKOWN(-1),
    Reserved(0),
    ICMP(1),//IP数据包在发送途中一旦发生异常导致无法到达对端目标地址时，需要给发送端发送一个发生异常的通知。ICMP就是为这一功能而制定的。它有时也被用来诊断网络的健康状况。
    IGMP(2),
    IP(4),
    TCP(6),
    UDP(17),
    IGRP(88),
    OSPF(89),
    MplsInIp(137);

    companion object {
        fun parseProtocol(v: Int): Protocol {
            for (e in values()) {
                if (e.typeValue == v) {
                    return e
                }
            }
            return UNKOWN
        }
    }

}

enum class IpVersion(val typeValue: Int) {
    UNKOWN(-1),
    V4(0b0100),
    V6(0b0110);

    companion object {
        fun parseIpVersion(v: Int): IpVersion {
            for (e in values()) {
                if (e.typeValue == v) {
                    return e
                }
            }
            return UNKOWN
        }
    }
}

fun ByteBuffer.getIpHeader(): IpHeader {
    val buffer = this
    val versionAndIhl = buffer.getUByte()
    val version = IpVersion.parseIpVersion(versionAndIhl shr 4)
    val ihl = (versionAndIhl and 0x0f).toInt() * 4
    val typeOfService = TypeOfService(buffer.getUByte())
    val totalLength = buffer.getUShort()
    val identification = buffer.getUShort()
    val flagsAndFragment = buffer.getUShort()
    val flags = IpFlag(flagsAndFragment shr 13)
    val fragmentOffset = flagsAndFragment and 0b0001_1111_1111_1111
    val ttl = buffer.getUByte()
    val protocol = Protocol.parseProtocol(buffer.getUByte())
    val headerChecksum = buffer.getUShort()
    val sourceAddresses = InetAddress.getByAddress(buffer.getBytes(4))
    val destinationAddresses = InetAddress.getByAddress(buffer.getBytes(4))

    val ipHeader = IpHeader(
        version, ihl, typeOfService, totalLength,
        identification, flags, fragmentOffset, ttl,
        protocol, headerChecksum, sourceAddresses, destinationAddresses
    )
    val optionSize = ihl - IP4_HEADER_SIZE
    if (optionSize > 0) {
        //options

    }
    return ipHeader
}

fun IpHeader.toByteBuffer(): ByteBuffer {
    val buffer = ByteBuffer.allocate(ihl)
    val versionAndIhl = ((version.typeValue shl 4) or (ihl/4)).toByte()
    buffer.putUByte(versionAndIhl)
    buffer.putUByte(typeOfService.byteValue.toByte())
    buffer.putUShort(totalLength)
    buffer.putUShort(identification)
    val flagsAndFragment = (flags.byteValue shl 13) or fragmentOffset
    buffer.putUShort(flagsAndFragment)
    buffer.putUByte(ttl.toByte())
    buffer.putUByte(protocol.typeValue.toByte())
    buffer.putUShort(headerChecksum)
    buffer.put(sourceAddresses.address)
    buffer.put(destinationAddresses.address)
    buffer.flip()
    return buffer
}

data class IpHeader(
    val version: IpVersion,//4bits
    val ihl: Int,//4bits 描述IP包头的长度,由固定长度20字节+可变长度40字节，以4bytes为一个单位，固定长度20字节的值为20/4=5，需要用4bits表示5即为0101
    val typeOfService: TypeOfService,//8bits
    val totalLength: Int,//16bits 可存65535大小
    val identification: Int,//16bits 主机每发一个报文，加1，分片重组时会用到该字段。
    val flags: IpFlag,//3bits
    val fragmentOffset: Int,//13bits 分片重组时会用到该字段。表示较长的分组在分片后，某片在原分组中的相对位置。以8个字节为偏移单位
    val ttl: Int,//8bits Time to Live
    val protocol: Protocol,//8bits
    val headerChecksum: Int,//16bits
    val sourceAddresses: InetAddress,//32bits
    val destinationAddresses: InetAddress//32bits
) {
    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |Version|  IHL  |Type of Service|          Total Length         |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |         Identification        |Flags|      Fragment Offset    |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |  Time to Live |    Protocol   |         Header Checksum       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                       Source Address                          |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                    Destination Address                        |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                    Options                    |    Padding    |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        Example Internet Datagram Header
     */
    override fun toString(): String {
        return "\"ipheader\":{\"version\":$version,\"ihl\":$ihl,\"typeOfService\":$typeOfService,\"totalLength\":$totalLength," +
                "\"identification\":$identification,\"flags\":$flags,\"fragmentOffset\":$fragmentOffset,\"ttl\":$ttl," +
                "\"protocol\":$protocol,\"headerChecksum\":$headerChecksum," +
                "\"sourceAddresses\":$sourceAddresses,\"destinationAddresses\":$destinationAddresses}"
    }
}

