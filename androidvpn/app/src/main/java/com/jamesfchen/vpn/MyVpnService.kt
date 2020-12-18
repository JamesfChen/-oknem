package com.jamesfchen.vpn
import android.R
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawskjamesf
 * @since: Sep/25/2018  Tue
 */
class MyVpnService : VpnService() {
    private val api: IMockApi = object : Stub() {
        @Throws(RemoteException::class)
        fun register(callback: IMockServerCallback?) {
        }
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
            val notificationIntent = Intent(this, LogActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            notificationManager.createNotificationChannel(chan)
            val notification: Notification =
                Notification.Builder(this, channelId)
                    .setContentTitle("this is title")
                    .setContentText("this is text")
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setTicker("this is ticker")
                    .build()
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind")
        return api.asBinder()
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
        if (intent != null) {
            a = intent.getStringExtra("cjf")
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
        private val TAG: String = Constants.TAG.toString() + "/MockFGService"
        fun bindAndStartService(
            activity: Context,
            connection: ServiceConnection?
        ) {
            val intent = Intent(activity, MockForegroundService::class.java)
            intent.putExtra("cjf", "cjf123412")
            activity.bindService(intent, connection!!, Context.BIND_AUTO_CREATE)
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
            val intent = Intent(activity, MockForegroundService::class.java)
            activity.unbindService(connection!!)
            activity.stopService(intent)
        }

        const val ONGOING_NOTIFICATION_ID = 100
        const val channelId = "channelId_00"
        const val channelName = "channelName_00"
    }
}
