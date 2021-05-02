package com.jamesfchen.vpn.protocol

const val HTTP1_HEADER_MAX_SIZE = 4096//4k，http层数据在tcp层会被分段处理，所以4k是多个个mtu的最大值
const val HTTP1_PAYLOAD_MAX_SIZE = 64 * 1024//64k,多个mtu的最大值
const val HTTP1_MAX_SIZE = 1460//一个mtu包的最大值