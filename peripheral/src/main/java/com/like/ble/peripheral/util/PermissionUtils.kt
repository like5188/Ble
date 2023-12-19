package com.like.ble.peripheral.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleExceptionDisabled
import com.like.ble.exception.BleExceptionPermission
import com.like.ble.util.checkPermissions
import com.like.ble.util.isBluetoothEnable
import com.like.ble.util.isBluetoothEnableAndSettingIfDisabled
import com.like.ble.util.requestPermissions

object PermissionUtils {
    private val advertisingPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        emptyArray()
    }

    /**
     * 检查广播蓝牙操作需要的环境，如果不满足，则去请求。
     * 1、如果没有打开蓝牙开关，则去打开。
     * 2、如果没有相关权限，则去请求。
     */
    suspend fun requestAdvertisingEnvironment(activity: ComponentActivity) {
        if (activity.requestPermissions(*advertisingPermissions)) {
            activity.isBluetoothEnableAndSettingIfDisabled()
        }
    }

    /**
     * 检查广播蓝牙操作需要的环境
     */
    fun checkAdvertisingEnvironment(context: Context): Boolean {
        return context.checkPermissions(*advertisingPermissions) && context.isBluetoothEnable()
    }

    /**
     * 检查广播蓝牙操作需要的环境，如果不满足，则抛异常。
     */
    internal fun checkAdvertisingEnvironmentOrThrow(context: Context) {
        if (!context.checkPermissions(*advertisingPermissions)) {
            throw BleExceptionPermission
        }
        if (!context.isBluetoothEnable()) {
            throw BleExceptionDisabled
        }
    }

}
