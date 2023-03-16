package com.like.ble.executor

import androidx.activity.ComponentActivity
import com.like.ble.exception.BleException
import com.like.ble.util.PermissionUtils
import com.like.ble.util.isBluetoothEnable
import com.like.ble.util.isBluetoothEnableAndSettingIfDisabled

/**
 * 蓝牙命令真正的执行者。
 */
abstract class BaseExecutor {
    /**
     * 释放资源
     */
    abstract fun close()

    protected fun checkEnvironment(activity: ComponentActivity, vararg permissions: String): Boolean {
        if (!activity.isBluetoothEnable()) {
            return false
        }
        if (!PermissionUtils.checkPermissions(activity, *permissions)) {
            return false
        }
        return true
    }

    @Throws(BleException::class)
    protected suspend fun checkEnvironmentOrThrowBleException(activity: ComponentActivity, vararg permissions: String) {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity, *permissions)) {
            throw BleException("蓝牙权限被拒绝")
        }
    }

}
