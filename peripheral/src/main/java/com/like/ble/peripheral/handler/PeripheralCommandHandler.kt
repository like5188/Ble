package com.like.ble.peripheral.handler

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.command.Command
import com.like.ble.handler.CommandHandler
import com.like.ble.peripheral.executor.AdvertisingCommandExecutor
import com.like.ble.executor.ICommandExecutor
import com.like.common.util.activityresultlauncher.requestMultiplePermissions

/**
 * 蓝牙外围设备相关命令处理。
 */
class PeripheralCommandHandler(activity: ComponentActivity) : CommandHandler(activity) {
    private val mAdvertisingCommandExecutor: ICommandExecutor by lazy { AdvertisingCommandExecutor(mActivity) }

    override suspend fun onExecute(command: Command): Boolean {
        val checkPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            mActivity.requestMultiplePermissions(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            ).all { it.value }
        } else {
            mActivity.requestMultiplePermissions(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
            ).all { it.value }
        }

        if (!checkPermissions) {
            command.errorAndComplete("蓝牙权限被拒绝")
            return false
        }

        command.mCommandExecutor = mAdvertisingCommandExecutor
        return true
    }

    override fun onClose() {
        mAdvertisingCommandExecutor.close()
    }

}
