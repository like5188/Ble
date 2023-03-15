package com.like.ble.peripheral.util

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.common.util.activityresultlauncher.requestMultiplePermissions

object PermissionUtils {
    suspend fun checkPermissions(activity: ComponentActivity): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        return activity.requestMultiplePermissions(*permissions).all { it.value }
    }
}