package com.like.ble.peripheral.executor

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.command.CloseCommand
import com.like.ble.command.Command
import com.like.ble.executor.CommandExecutor
import com.like.ble.peripheral.state.AdvertisingState
import com.like.ble.state.IState
import com.like.common.util.activityresultlauncher.requestMultiplePermissions

/**
 * 蓝牙外围设备相关命令的执行者。
 */
class PeripheralExecutor(activity: ComponentActivity) : CommandExecutor(activity) {
    private val mAdvertisingState: IState by lazy { AdvertisingState(mActivity) }

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

        command.mState = mAdvertisingState
        return true
    }

    override fun onClose() {
        mAdvertisingState.close(CloseCommand())
    }

}
