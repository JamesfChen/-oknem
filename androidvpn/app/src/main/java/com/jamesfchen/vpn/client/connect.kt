package com.jamesfchen.vpn.client

import android.util.Log
import com.jamesfchen.vpn.Constants
import com.jamesfchen.vpn.threadFactory
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Dec/20/2020  Sun
 */
const val C_TAG = "${Constants.TAG}/cli"
// httpdns.n.netease.com:115.236.121.49

abstract class Connection {
    companion object {
        @Throws(ConnectException::class)
        fun createAndConnectOverTcp(ip: String, port: Int, aioSocket: Boolean): Connection {
            if (aioSocket) {
                val asynSocketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
                asynSocketChannel.connect(InetSocketAddress(ip, port)).get()
                return AioSocketConnection(asynSocketChannel)
            } else {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port))
                socket.keepAlive = true
                return BioSocketConnection(socket)
            }
        }

        fun <A> createAndConnectOverTcp(
            ip: String,
            port: Int,
            handler: CompletionHandler<Void, A?>,
            attachment: A? = null
        ): Connection {
            val asynSocketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
            asynSocketChannel.connect(InetSocketAddress(ip, port), attachment, handler)
            return AioSocketConnection(asynSocketChannel)

        }

        fun createAndConnectOverUdp(ip: String, port: Int, aioSocket: Boolean): Connection {
            if (aioSocket) {
//                val asynSocketChannel: AsynchronousSocketChannel = As.open()
//                asynSocketChannel.connect(InetSocketAddress(ip, port)).get()
//                return AioSocketConnection(asynSocketChannel)
                val socket = DatagramSocket()
                return BioUdpConnection(ip,port,socket)
            } else {
                val socket = DatagramSocket()
                return BioUdpConnection(ip,port,socket)
            }
        }

    }

    // -- asynchronous operations --
    abstract val remoteAddress: InetSocketAddress?
    abstract val localAddress: InetSocketAddress?
    abstract fun send(reqBuffer: ByteArray, block: (respBuffer: ByteBuffer) -> Unit)
    open fun <A> send(
        reqBuffer: ByteArray,
        handler: CompletionHandler<Int, A?>, attachment: A? = null
    ) {
    }

    abstract fun close()
}

object ConnectionPool {
    const val TAG = "${C_TAG}/conn_pool"
    private val executer = ThreadPoolExecutor(
        0,
        Int.MAX_VALUE,
        60L,
        TimeUnit.SECONDS,
        SynchronousQueue(),
        threadFactory("vpn connection pool", true)
    )

    private val connections = ConcurrentHashMap<String, Connection>()
    fun remove(key: String) {
        val c = connections[key]
        if (connections.contains(key)) {
            c?.close()
            connections.remove(key)
        }
    }

    @Throws(ConnectException::class)
    @Synchronized
    fun get(key: String): List<Any> {
//        val c = connections[key]
//        if (c?.remoteAddress != null && "${c.remoteAddress!!.address.hostAddress}:${c.remoteAddress!!.port}" == key) {
//            return listOf(c, true)
//        }
        val (destIp, destPort) = key.split(":")
        val myClient = Connection.createAndConnectOverTcp(
            destIp,
            destPort.toInt(),
            aioSocket = false
        )
        connections[key] = myClient

        return listOf(myClient, false)
    }
    @Throws(ConnectException::class)
    @Synchronized
    fun getOverUdp(key: String): List<Any> {
//        val c = connections[key]
//        if (c?.remoteAddress != null && "${c.remoteAddress!!.address.hostAddress}:${c.remoteAddress!!.port}" == key) {
//            return listOf(c, true)
//        }
        val (destIp, destPort) = key.split(":")
        val myClient = Connection.createAndConnectOverUdp(
            destIp,
            destPort.toInt(),
            aioSocket = false
        )
        connections[key] = myClient

        return listOf(myClient, false)
    }
}