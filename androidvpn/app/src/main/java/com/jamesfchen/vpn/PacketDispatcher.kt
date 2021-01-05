package com.jamesfchen.vpn

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Jan/05/2021  Tue
 */
class PacketDispatcher {
    companion object {
        private val executer = ThreadPoolExecutor(
            0,
            Int.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            SynchronousQueue(),
            threadFactory("vpn connection pool", true)
        )
    }
}