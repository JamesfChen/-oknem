package com.jamesfchen.vpn.protocol

import android.os.*
import android.util.Log
import com.jamesfchen.vpn.Constants
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
 */
const val T_TAG="${Constants.TAG}/tdp"
class TcpHandlerThread() : HandlerThread("tcp_thread") {

}

class TcpHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        Log.d(T_TAG,"tcp message")
    }

}