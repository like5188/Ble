package com.like.ble.peripheral.handler

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.command.Command
import com.like.ble.executor.ICommandExecutor
import com.like.ble.handler.CommandHandler
import com.like.ble.peripheral.executor.AdvertisingCommandExecutor
import com.like.common.util.activityresultlauncher.requestMultiplePermissions

/**
 * 蓝牙外围设备相关命令处理。
 */
class PeripheralCommandHandler(activity: ComponentActivity) : CommandHandler(activity) {
    private val mAdvertisingCommandExecutor: ICommandExecutor by lazy { AdvertisingCommandExecutor(mActivity) }

    override suspend fun onExecute(command: Command): Boolean {
        if (!checkPermissions(mActivity, command)) {
            command.errorAndComplete("蓝牙权限被拒绝")
            return false
        }

        command.mCommandExecutor = mAdvertisingCommandExecutor
        return true
    }

    override fun onClose() {
        mAdvertisingCommandExecutor.close()
    }

    private suspend fun checkPermissions(activity: ComponentActivity, command: Command): Boolean {
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
