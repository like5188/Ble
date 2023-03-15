package com.like.ble.central.util

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.common.util.activityresultlauncher.requestMultiplePermissions

object PermissionUtils {
    suspend fun checkPermissions(activity: ComponentActivity, isScan: Boolean): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            if (isScan) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
            }
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