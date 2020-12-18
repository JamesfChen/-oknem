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
//    private val api: IMockApi = object : Stub() {
//        @Throws(RemoteException::class)
//        fun register(callback: IMockServerCallback?) {
//        }
//    }

    override fun onCreate() {
        super.onCreate()
        setupVPN()
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
    private  val VPN_ADDRESS = "10.0.0.2" // Only IPv4 support for now
    private  val VPN_ROUTE = "0.0.0.0" // Intercept everything
    private var vpnInterface: ParcelFileDescriptor? = null
    private fun setupVPN() {
        try {
            if (vpnInterface == null) {
                val builder = Builder()
                builder.addAddress(VPN_ADDRESS, 32)
                builder.addRoute(VPN_ROUTE, 0)
                try {
                    builder.addAllowedApplication("com.netease.cloudmusic")
                    builder.addAllowedApplication("com.netease.cloudmusic.lite")
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.d(TAG, "未检测到网易云音乐")
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.d(TAG, "未检测到网易云音乐极速版")
                }
                vpnInterface = builder.setSession(getString(R.string.app_name)).establish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Easy163 VPN 启动失败")
            exitProcess(0)
        }
    }


//    override fun onBind(intent: Intent): IBinder? {
//        Log.d(TAG, "onBind")
//        return api.asBinder()
//    }

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
        if (intent != null) {
            a = intent.getStringExtra("vpn")
        }
        Log.d(
            TAG,
            "startId:$startId intent value:$a"
        )
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    companion object {
        private val TAG: String = Constants.TAG + "/VpnService"
        fun bindAndStartService(
            activity: Context,
            connection: ServiceConnection? = null
        ) {
            val intent = Intent(activity, MyVpnService::class.java)
            intent.putExtra("vpn", "123412")
            connection?.let {
                activity.bindService(intent, it, Context.BIND_AUTO_CREATE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent)
            } else {
                activity.startService(intent)
            }
        }

        fun unbindAndStopService(
            activity: Context,
            connection: ServiceConnection?
        ) {
            val intent = Intent(activity, MyVpnService::class.java)
            activity.unbindService(connection!!)
            activity.stopService(intent)
        }

        const val ONGOING_NOTIFICATION_ID = 100
        const val channelId = "channelId_00"
        const val channelName = "channelName_00"
    }
}
