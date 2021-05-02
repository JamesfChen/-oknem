package com.jamesfchen.vpn.protocol

import java.nio.ByteBuffer

const val HTTP2_HEADER_MAX_SIZE = 4096//4k，http层数据在tcp层会被分段处理，所以4k是多个个mtu的最大值
const val HTTP2_PAYLOAD_MAX_SIZE = 16_777_215//2^14(16,384) 到 2^24-1(16,777,215),多个mtu的最大值
const val HTTP2_MAX_SIZE = 1460//一个mtu包的最大值

/**
 * [Hypertext Transfer Protocol Version 2 (HTTP/2)
](https://httpwg.org/specs/rfc7540.html)
 */
data class FrameHeader(
    val length: Int,//3字节，无符号来表示payload的大小
    val type: Int,
    //    val reserved: Int// 预留1bit
    val flags: Int, val streamId: Int
) : ApplicationLayerHeader {
    override fun toByteBuffer(): ByteBuffer {
        TODO("Not yet implemented")
    }
    //  0                   1                   2                   3
    //  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    // |                 Length (24)                   |
    // +---------------+---------------+---------------+
    // |   Type (8)    |   Flags (8)   |
    // +-+-+-----------+---------------+-------------------------------+
    // |R|                 Stream Identifier (31)                      |
    // +=+=============================================================+
    // |                   Frame Payload (0...)                      ...
    // +---------------------------------------------------------------+

}

fun ByteBuffer.getFrameHeader(): FrameHeader {
    TODO("Not yet implemented")
}

enum class FrameType(val byteValue: Int) {
    UNKOWN(-1),
    TYPE_DATA(0x0), //数据帧
    TYPE_HEADERS(0x1), //头部帧
    TYPE_PRIORITY(0x2),  //服务器处理流的优先级
    TYPE_RST_STREAM(0x3), //终止流帧
    TYPE_SETTINGS(0x4), //网络配置帧
    TYPE_PUSH_PROMISE(0x5),
    TYPE_PING(0x6), //心跳，rtt帧
    TYPE_GOAWAY(0x7),
    TYPE_WINDOW_UPDATE(0x8), //流量控制帧，为什么需要这个帧？由于http2中一个tcp可以有多个stream同时写入任意个请求，所以需要控制网络中的流量，避免失控。
    TYPE_CONTINUATION(0x9);

    companion object {
        fun parseType(v: Int): FrameType {
            for (e in values()) {
                if (e.byteValue == v) {
                    return e
                }
            }
            return UNKOWN
        }
    }

}

data class DataFrame(val i: Int) {
    /*
     +---------------+
     |Pad Length? (8)|
     +---------------+-----------------------------------------------+
     |                            Data (*)                         ...
     +---------------------------------------------------------------+
     |                           Padding (*)                       ...
     +---------------------------------------------------------------+
     */
}

data class HeaderFrame(val i: Int) {
    /*
 +---------------+
 |Pad Length? (8)|
 +-+-------------+-----------------------------------------------+
 |E|                 Stream Dependency? (31)                     |
 +-+-------------+-----------------------------------------------+
 |  Weight? (8)  |
 +-+-------------+-----------------------------------------------+
 |                   Header Block Fragment (*)                 ...
 +---------------------------------------------------------------+
 |                           Padding (*)                       ...
 +---------------------------------------------------------------+
     */

}

data class PriorityFrame(val i: Int) {
    /*
 +-+-------------------------------------------------------------+
 |E|                  Stream Dependency (31)                     |
 +-+-------------+-----------------------------------------------+
 |   Weight (8)  |
 +-+-------------+
     */
}

data class RstStreamFrame(val i: Int) {
    /*
 +---------------------------------------------------------------+
 |                        Error Code (32)                        |
 +---------------------------------------------------------------+
     */
}

data class SettingsFrame(val i: Int) {
    /*
 +-------------------------------+
 |       Identifier (16)         |
 +-------------------------------+-------------------------------+
 |                        Value (32)                             |
 +---------------------------------------------------------------+
     */
}

data class PushPromiseFrame(val i: Int) {
    /*
 +---------------+
 |Pad Length? (8)|
 +-+-------------+-----------------------------------------------+
 |R|                  Promised Stream ID (31)                    |
 +-+-----------------------------+-------------------------------+
 |                   Header Block Fragment (*)                 ...
 +---------------------------------------------------------------+
 |                           Padding (*)                       ...
 +---------------------------------------------------------------+
     */
}

data class PingFrame(val i: Int) {
    /*
 +---------------------------------------------------------------+
 |                                                               |
 |                      Opaque Data (64)                         |
 |                                                               |
 +---------------------------------------------------------------+
     */
}

data class GoAwayFrame(val i: Int) {
    /*
 +-+-------------------------------------------------------------+
 |R|                  Last-Stream-ID (31)                        |
 +-+-------------------------------------------------------------+
 |                      Error Code (32)                          |
 +---------------------------------------------------------------+
 |                  Additional Debug Data (*)                    |
 +---------------------------------------------------------------+
     */
}

data class WindowUpdateFrame(val i: Int) {
    /*
 +-+-------------------------------------------------------------+
 |R|              Window Size Increment (31)                     |
 +-+-------------------------------------------------------------+
     */
}

data class ContinuationFrame(val i: Int) {
    /*
 +---------------------------------------------------------------+
 |                   Header Block Fragment (*)                 ...
 +---------------------------------------------------------------+
     */
}

enum class ErrorCode constructor(val httpCode: Int) {
    /** Not an error!  */
    NO_ERROR(0),

    PROTOCOL_ERROR(1),

    INTERNAL_ERROR(2),

    FLOW_CONTROL_ERROR(3),

    SETTINGS_TIMEOUT(4),

    STREAM_CLOSED(5),

    FRAME_SIZE_ERROR(6),

    REFUSED_STREAM(7),

    CANCEL(8),

    COMPRESSION_ERROR(9),

    CONNECT_ERROR(0xa),

    ENHANCE_YOUR_CALM(0xb),

    INADEQUATE_SECURITY(0xc),

    HTTP_1_1_REQUIRED(0xd);

    companion object {
        fun fromHttp2(code: Int): ErrorCode? = values().find { it.httpCode == code }
    }
}

enum class Http2StreamStatus {
    /*
                             +--------+
                     send PP |        | recv PP
                    ,--------|  idle  |--------.
                   /         |        |         \
                  v          +--------+          v
           +----------+          |           +----------+
           |          |          | send H /  |          |
    ,------| reserved |          | recv H    | reserved |------.
    |      | (local)  |          |           | (remote) |      |
    |      +----------+          v           +----------+      |
    |          |             +--------+             |          |
    |          |     recv ES |        | send ES     |          |
    |   send H |     ,-------|  open  |-------.     | recv H   |
    |          |    /        |        |        \    |          |
    |          v   v         +--------+         v   v          |
    |      +----------+          |           +----------+      |
    |      |   half   |          |           |   half   |      |
    |      |  closed  |          | send R /  |  closed  |      |
    |      | (remote) |          | recv R    | (local)  |      |
    |      +----------+          |           +----------+      |
    |           |                |                 |           |
    |           | send ES /      |       recv ES / |           |
    |           | send R /       v        send R / |           |
    |           | recv R     +--------+   recv R   |           |
    | send R /  `----------->|        |<-----------'  send R / |
    | recv R                 | closed |               recv R   |
    `----------------------->|        |<----------------------'
                             +--------+

       send:   endpoint sends this frame
       recv:   endpoint receives this frame

       H:  HEADERS frame (with implied CONTINUATIONs)
       PP: PUSH_PROMISE frame (with implied CONTINUATIONs)
       ES: END_STREAM flag
       R:  RST_STREAM frame
     */

}