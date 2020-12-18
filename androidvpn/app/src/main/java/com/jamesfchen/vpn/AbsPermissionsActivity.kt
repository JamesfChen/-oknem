package com.jamesfchen.vpn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*


abstract class AbsPermissionsActivity : AppCompatActivity() {
    private val mPermissions: List<String> =
        object : ArrayList<String>() {
            init {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    add(Manifest.permission.READ_PHONE_NUMBERS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                    add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(Manifest.permission.FOREGROUND_SERVICE)
                }
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_PHONE_STATE)
                add(Manifest.permission.INTERNET)
                add(Manifest.permission.ACCESS_NETWORK_STATE)
                add(Manifest.permission.ACCESS_WIFI_STATE)
                add(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)
                add(Manifest.permission.CHANGE_WIFI_STATE)
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN) /* Manifest.permission.BLUETOOTH_PRIVILEGED,*/
                add(Manifest.permission.RECEIVE_BOOT_COMPLETED)
                add(Manifest.permission.RECORD_AUDIO)
                add(Manifest.permission.CAMERA)
            }
        }
    private val mFailurePermissions: MutableList<String> =
        ArrayList()
    private var mPermissionRequestCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mPermissionRequestCount = savedInstanceState.getInt(
                KEY_PERMISSIONS_REQUEST_COUNT,
                0
            )
        }
        requestPermissionsIfNecessary()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            KEY_PERMISSIONS_REQUEST_COUNT,
            mPermissionRequestCount
        )
    }

    fun requestPermissionsIfNecessary() {
        if (!hasPermission()) {
            if (mPermissionRequestCount < MAX_NUMBER_REQUEST_PERMISSIONS) {
                ++mPermissionRequestCount
                requestPermission()
            } else {
                val sb = StringBuffer()
                for (p in mFailurePermissions) {
                    sb.append(p)
                    sb.append('\n')
                }
                Toast.makeText(this, "need permission:$sb", Toast.LENGTH_LONG).show()
            }
        } else {
            onRequestPermissionsResult()
        }
    }

    private fun hasPermission(): Boolean {
        var hasPermission = true
        for (permission in mPermissions) {
            val b = ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            if (!b) {
                mFailurePermissions.add(permission)
            }
            hasPermission = hasPermission and b
        }
        return hasPermission
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val size = mPermissions.size
            val a = arrayOfNulls<String>(size)
            for (i in 0 until size) {
                a[i] = mPermissions[i]
            }
            requestPermissions(a, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_CODE_PERMISSIONS == requestCode) {
            requestPermissionsIfNecessary()
        }
    }

    protected abstract fun onRequestPermissionsResult()

    companion object {
        private const val MAX_NUMBER_REQUEST_PERMISSIONS = 2
        private const val REQUEST_CODE_PERMISSIONS = 101
        private const val KEY_PERMISSIONS_REQUEST_COUNT = "KEY_PERMISSIONS_REQUEST_COUNT"
    }
}
