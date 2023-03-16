package com.like.ble.central.connect

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.like.common.util.activityresultlauncher.requestMultiplePermissions

object PermissionUtils {
    fun checkPermissions(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun requestPermissions(activity: ComponentActivity): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
        return activity.requestMultiplePermissions(*permissions).all { it.value }
    }
}