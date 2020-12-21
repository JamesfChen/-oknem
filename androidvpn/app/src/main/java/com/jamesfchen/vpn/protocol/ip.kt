package com.jamesfchen.vpn.protocol

import android.os.ParcelFileDescriptor
import java.io.Closeable
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
 * [IP报文格式](http://www.023wg.com/message/message/cd_feature_ip_message_format.html)
 * [IP协议头部](https://zhangbinalan.gitbooks.io/protocol/content/ipxie_yi_tou_bu.html)
 * [TCP 协议简介](http://www.ruanyifeng.com/blog/2017/06/tcp-protocol.html#:~:text=%E4%BB%A5%E5%A4%AA%E7%BD%91%E6%95%B0%E6%8D%AE%E5%8C%85%EF%BC%88packet,%E6%9C%80%E5%A4%9A%E4%B8%BA1480%E5%AD%97%E8%8A%82%E3%80%82)
 */

