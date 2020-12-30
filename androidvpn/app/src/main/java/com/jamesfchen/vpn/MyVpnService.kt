package com.jamesfchen.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlin.system.exitProcess

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawskjamesf
 * @since: Sep/25/2018  Tue
 */
class MyVpnService : VpnService() {

    companion object {
        private val TAG: String = Constants.TAG + "/VpnService"
        fun start(activity: Context, intent: Intent? = null) {
            val realIntent = intent ?: Intent(activity, MyVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(realIntent)
            } else {
                activity.startService(realIntent)
            }
        }

        private fun bind(activity: Context, intent: Intent, connection: ServiceConnection) {
            activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun bindAndStart(activity: Context, intent: Intent? = null, connection: ServiceConnection) {
            val realIntent = intent ?: Intent(activity, MyVpnService::class.java)
            start(activity, realIntent)
            bind(activity, realIntent, connection)
        }

        private fun unbind(activity: Context, connection: ServiceConnection) {
            activity.unbindService(connection)
        }

        fun stop(activity: Context, intent: Intent? = null) {
            val realIntent = intent ?: Intent(activity, MyVpnService::class.java)
            activity.stopService(realIntent)
        }

        fun unbindAndStop(
            activity: Context,
            intent: Intent? = null,
            connection: ServiceConnection
        ) {
            val realIntent = intent ?: Intent(activity, MyVpnService::class.java)
            activity.unbindService(connection)
            activity.stopService(realIntent)
        }

        const val ONGOING_NOTIFICATION_ID = 100
        const val channelId = "channelId_00"
        const val channelName = "channelName_00"
        const val ALLOW_PKG_NAME_LIST = "allow_pkg_name_list"
        const val DISALLOW_PKG_NAME_LIST = "disallow_pkg_name_list"

        private const val VPN_ADDRESS = "10.0.0.2" // Only IPv4 support for now
        private const val VPN_ROUTE = "0.0.0.0" // Intercept everything
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            notificationManager.createNotificationChannel(chan)
            val notification: Notification =
                Notification.Builder(this, channelId)
                    .setContentTitle("this is title")
                    .setContentText("this is text")
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker("this is ticker")
                    .build()
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    private fun createVpn(intent: Intent?): ParcelFileDescriptor? {
        intent ?: return null
        try {
            val builder = Builder()
            try {
                builder.apply {
                    addAddress(VPN_ADDRESS, 32)
                    addRoute(VPN_ROUTE, 0)
                    intent.getStringArrayListExtra(ALLOW_PKG_NAME_LIST)
                        ?.forEach {
                            Log.d(TAG, "allow:$it")
                            packageManager.getPackageInfo(it, 0)
                            addAllowedApplication(it)
                        }
                    intent.getStringArrayListExtra(DISALLOW_PKG_NAME_LIST)
                        ?.forEach {
                            addDisallowedApplication(it)
                        }
//                    addDisallowedApplication(pkg_name)
//                    addDnsServer(dns_address)
//                    addSearchDomain(domain)
//                    allowBypass()
//                    allowFamily(AF_INET)
//                    setBlocking(true)
//                    setHttpProxy(proxyinfo)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        setMetered(true)
                    }
//                    setMtu(mtu)
//                    setSession(session)
//                    setUnderlyingNetworks(networks)
                    //                setConfigureIntent()
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "未检测到网易云音乐")
            }
            return builder.setSession(getString(R.string.app_name)).establish()
        } catch (e: Exception) {
            Log.e(TAG, "VPN 启动失败")
            exitProcess(0)
        }
    }

    /**
     * 当MockService进程被kill掉，经过短暂的几秒系统会自动重启进程
     * START_STICKY：被kill掉之后，service进程会重启，会执行onCreate，onBind，onStartComman. Intent将为null
     * START_NOT_STICKY:
     * - 当service采用绑定启动，那么被kill掉之后，service进程会重启，会执行onCreate，onBind，不会执行onStartCommand
     * - 当service没有采用绑定启动，那么被kill之后不会再重启
     * START_REDELIVER_INTENT：被kill掉之后，service进程会重启，会执行onCreate，onBind，但是不会执行onStartCommand. Intent将重新传
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var a = "-1"
        createVpn(intent)?.let {
            val vpnHandler = VpnHandlerThread(it)
            vpnHandler.start()
        }
        Log.d(TAG, "startId:$startId intent value:$a")
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

}
