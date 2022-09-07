package com.like.ble.central.handler

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.central.command.AddressCommand
import com.like.ble.central.command.StartScanCommand
import com.like.ble.central.command.StopScanCommand
import com.like.ble.central.executor.ConnectCommandExecutor
import com.like.ble.central.executor.ScanCommandExecutor
import com.like.ble.command.Command
import com.like.ble.executor.ICommandExecutor
import com.like.ble.handler.CommandHandler
import com.like.common.util.activityresultlauncher.requestMultiplePermissions
import com.like.common.util.activityresultlauncher.requestPermission

/**
 * 蓝牙中心设备相关命令处理。
 */
class CentralCommandHandler(activity: ComponentActivity) : CommandHandler(activity) {
    private var mCurCommandExecutor: ICommandExecutor? = null
    private val mScanCommandExecutor: ICommandExecutor by lazy { ScanCommandExecutor(mActivity) }
    private val mConnectCommandExecutorMap = mutableMapOf<String, ICommandExecutor>()

    override suspend fun onExecute(command: Command): Boolean {
        val checkPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            when (command) {
                is StartScanCommand, is StopScanCommand -> {
                    mActivity.requestMultiplePermissions(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ).all { it.value }
                }
                else -> {
                    mActivity.requestPermission(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            mActivity.requestMultiplePermissions(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ).all { it.value }
        } else {
            mActivity.requestMultiplePermissions(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ).all { it.value }
        }

        if (!checkPermissions) {
            command.errorAndComplete("蓝牙权限被拒绝")
            return false
        }

        val state = getStateByCommand(command)
        if (state == null) {
            command.errorAndComplete("更新蓝牙状态失败，无法执行命令：$command")
            return false
        }

        mCurCommandExecutor = state
        command.mCommandExecutor = state
        return true
    }

    override fun onClose() {
        mScanCommandExecutor.close()
        mConnectCommandExecutorMap.forEach {
            it.value.close()
        }
        mConnectCommandExecutorMap.clear()
        mCurCommandExecutor = null
    }

    private fun getStateByCommand(command: Command): ICommandExecutor? {
        return when (command) {
            is StartScanCommand, is StopScanCommand -> {
                mScanCommandExecutor
            }
            is AddressCommand -> {
                if (!mConnectCommandExecutorMap.containsKey(command.address)) {
                    mConnectCommandExecutorMap[command.address] = ConnectCommandExecutor(mActivity)
                }
                mConnectCommandExecutorMap[command.address]
            }
            else -> mCurCommandExecutor
        }
    }

}