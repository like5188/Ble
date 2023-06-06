package com.like.ble.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.like.ble.exception.BleExceptionDisabled
import com.like.ble.exception.BleExceptionPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PermissionUtils {
    private val advertisingPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        emptyArray()
    }
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
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun checkAdvertisingEnvironment(context: Context): Boolean {
        return checkEnvironment(context, advertisingPermissions)
    }

    fun checkConnectEnvironment(context: Context): Boolean {
        return checkEnvironment(context, connectPermissions)
    }

    fun checkScanEnvironment(context: Context): Boolean {
        return checkEnvironment(context, scanPermissions)
    }

    fun checkAdvertisingEnvironmentOrThrow(context: Context) {
        checkEnvironmentOrThrow(context, advertisingPermissions)
    }

    fun checkConnectEnvironmentOrThrow(context: Context) {
        checkEnvironmentOrThrow(context, connectPermissions)
    }

    fun checkScanEnvironmentOrThrow(context: Context) {
        checkEnvironmentOrThrow(context, scanPermissions)
    }

    suspend fun requestAdvertisingEnvironment(activity: ComponentActivity) {
        requestEnvironment(activity, advertisingPermissions)
    }

    suspend fun requestConnectEnvironment(activity: ComponentActivity) {
        requestEnvironment(activity, connectPermissions)
    }

    suspend fun requestScanEnvironment(activity: ComponentActivity) {
        requestEnvironment(activity, scanPermissions)
    }

    private fun checkEnvironment(context: Context, permissions: Array<out String>): Boolean {
        return checkPermissions(context, *permissions) && context.isBluetoothEnable()
    }

    private fun checkEnvironmentOrThrow(context: Context, permissions: Array<out String>) {
        if (!checkPermissions(context, *permissions)) {
            throw BleExceptionPermission
        }
        if (!context.isBluetoothEnable()) {
            throw BleExceptionDisabled
        }
    }

    /**
     * 检查蓝牙操作需要的环境，如果不满足，则去请求。
     * 1、如果没有打开蓝牙开关，则去打开。
     * 2、如果没有相关权限，则去请求。
     */
    private suspend fun requestEnvironment(activity: ComponentActivity, permissions: Array<out String>) = withContext(Dispatchers.Main) {
        if (requestPermissions(activity, *permissions)) {
            activity.isBluetoothEnableAndSettingIfDisabled()
        }
    }

    /**
     * 检查是否拥有指定的权限
     */
    private fun checkPermissions(context: Context, vararg permissions: String): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * 检查并请求指定的权限
     * @return true：同意了所有权限；false：没有同意所有权限；
     */
    private suspend fun requestPermissions(activity: ComponentActivity, vararg permissions: String): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        return activity.requestMultiplePermissions(*permissions).all { it.value }
    }

}
