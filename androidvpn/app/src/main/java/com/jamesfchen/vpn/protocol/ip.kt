package com.jamesfchen.vpn.protocol

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
 */

val buffer: ByteBuffer = ByteBuffer.allocate(100)

//3bit
enum class PrecedenceType(value: Int) {
    Routine(0b000),//普通
    Priority(0b001),//优先的
    Immediate(0b010),//立即的发送
    Flash(0b011),//闪电式的
    Flash_Override(0b100),//比闪电还闪电式的
    CRITIC_ECP(0b101),//CRITIC/ECP
    Internetwork_Control(0b110),//网间控制
    Network_Control(0b111);//网络控制
}

//D 时延: 0:普通 1:延迟尽量小
val normal_delay = 0
val low_delay = 1

//T 吞吐量: 0:普通 1:流量尽量大
val normal_throughput = 0
val high_throughput = 1

//R 可靠性: 0:普通 1:可靠性尽量大
val normal_relibility = 0
val high_relibility = 1
class ServiceType{
//    val PrecedenceType:PrecedenceType
//    val d:Int
//    val t:Int
//    val r:Int
}
class IpPacket(buffer: ByteBuffer) {
    val version: Int//4bits
    val ihl: Int//4bits
    val typeOfService: Int//8bits

    init {
        version = buffer[0].toInt() and 0xf0
        ihl = buffer[0].toInt() and 0x0f
        typeOfService = buffer[1].toInt()
    }
}
