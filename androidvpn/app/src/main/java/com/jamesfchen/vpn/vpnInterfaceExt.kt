package com.jamesfchen.vpn

import android.content.pm.PackageManager
import android.net.VpnService
import android.util.Log
import kotlin.system.exitProcess

private const val TAG: String = Constants.TAG + "/vpninterface"
private const val VPN_ADDRESS = "10.0.0.2" // Only IPv4 support for now
private const val VPN_ROUTE = "0.0.0.0" // Intercept everything

private fun create(session:String) {
    try {
        val builder = VpnService.Builder()
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
        return builder.setSession(session).establish()
//        vpnInterface = builder.setSession(getString(R.string.app_name)).establish()
    } catch (e: Exception) {
        Log.e(TAG, "Easy163 VPN 启动失败")
        exitProcess(0)
    }
}