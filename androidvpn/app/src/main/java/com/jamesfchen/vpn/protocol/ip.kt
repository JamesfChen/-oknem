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
 */

