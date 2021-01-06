package com.jamesfchen.vpn

import java.util.concurrent.ThreadFactory

/**
 * Copyright ® $ 2021
 * All right reserved.
 */
fun threadFactory(
    name: String,
    daemon: Boolean
): ThreadFactory = ThreadFactory { runnable ->
    Thread(runnable, name).apply {
        isDaemon = daemon
    }
}