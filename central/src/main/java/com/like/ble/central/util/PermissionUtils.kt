package com.like.ble.central.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleExceptionDisabled
import com.like.ble.exception.BleExceptionGpsClosed
import com.like.ble.exception.BleExceptionPermission
import com.like.ble.util.checkPermissions
import com.like.ble.util.isBluetoothEnable
import com.like.ble.util.isBluetoothEnableAndSettingIfDisabled
import com.like.ble.util.requestPermissions

object PermissionUtils {
    private val connectPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    private val scanPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,// 获取设备名字的时候需要这个权限，BluetoothDevice.getName()
            Manifest.permission.ACCESS_FINE_LOCATION// 如果使用了<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation"/>，那么可以去掉此权限。
        )
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * 检查扫描蓝牙操作需要的环境，如果不满足，则去请求。
     * 1、如果没有打开蓝牙开关，则去打开。
     * 2、如果没有相关权限，则去请求。
     * 3、如果没有打开定位服务，则去打开。
     * 注意：此方法包含了[requestConnectEnvironment]方法的操作，不需要再重复调用了，重复调用会造成多次跳转去打开蓝牙或者打开定位服务
     */
    suspend fun requestScanEnvironment(activity: ComponentActivity) {
        if (activity.requestPermissions(*scanPermissions)) {
            activity.isBluetoothEnableAndSettingIfDisabled() && activity.isGpsOpenAndSettingIfClosed()
        }
    }

    /**
     * 检查中连接需要的环境，如果不满足，则去请求。
     * 1、如果没有打开蓝牙开关，则去打开。
     * 2、如果没有相关权限，则去请求。
     * 注意：如果调用了[requestScanEnvironment]方法，就不需要再调用此方法了，重复调用会造成多次跳转去打开蓝牙或者打开定位服务
     */
    suspend fun requestConnectEnvironment(activity: ComponentActivity) {
        if (activity.requestPermissions(*connectPermissions)) {
            activity.isBluetoothEnableAndSettingIfDisabled()
        }
    }

    /**
     * 检查连接蓝牙操作需要的环境
     */
    fun checkConnectEnvironment(context: Context): Boolean {
        return context.checkPermissions(*connectPermissions) && context.isBluetoothEnable()
    }

    /**
     * 检查扫描蓝牙操作需要的环境
     */
    fun checkScanEnvironment(context: Context): Boolean {
        return context.checkPermissions(*scanPermissions) && context.isBluetoothEnable() && context.isGpsOpen()
    }

    /**
     * 检查连接蓝牙操作需要的环境，如果不满足，则抛异常。
     */
    internal fun checkConnectEnvironmentOrThrow(context: Context) {
        if (!context.checkPermissions(*connectPermissions)) {
            throw BleExceptionPermission
        }
        if (!context.isBluetoothEnable()) {
            throw BleExceptionDisabled
        }
    }


    /**
     * 检查扫描蓝牙操作需要的环境，如果不满足，则抛异常。
     */
    internal fun checkScanEnvironmentOrThrow(context: Context) {
        if (!context.checkPermissions(*scanPermissions)) {
            throw BleExceptionPermission
        }
        if (!context.isBluetoothEnable()) {
            throw BleExceptionDisabled
        }
        if (!context.isGpsOpen()) {
            throw BleExceptionGpsClosed
        }
    }

}
