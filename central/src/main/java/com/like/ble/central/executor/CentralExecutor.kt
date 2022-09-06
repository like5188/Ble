package com.like.ble.central.executor

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.central.command.AddressCommand
import com.like.ble.central.command.StartScanCommand
import com.like.ble.central.command.StopScanCommand
import com.like.ble.central.state.ConnectState
import com.like.ble.central.state.ScanState
import com.like.ble.command.CloseCommand
import com.like.ble.command.Command
import com.like.ble.executor.CommandExecutor
import com.like.ble.state.IState
import com.like.common.util.activityresultlauncher.requestMultiplePermissions
import com.like.common.util.activityresultlauncher.requestPermission

/**
 * 蓝牙中心设备相关命令的执行者。
 */
class CentralExecutor(activity: ComponentActivity) : CommandExecutor(activity) {
    private var mCurState: IState? = null
    private val mScanState: IState by lazy { ScanState(mActivity) }
    private val mConnectStateMap = mutableMapOf<String, IState>()

    override suspend fun onExecute(command: Command): Boolean {
        val checkPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            when (command) {
                is StartScanCommand, is StopScanCommand -> {
                    mActivity.requestPermission(Manifest.permission.BLUETOOTH_SCAN)
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

        mCurState = state
        command.mState = state
        return true
    }

    override fun onClose() {
        mScanState.close(CloseCommand())
        mConnectStateMap.forEach {
            it.value.close(CloseCommand())
        }
        mConnectStateMap.clear()
        mCurState = null
    }

    private fun getStateByCommand(command: Command): IState? {
        return when (command) {
            is StartScanCommand, is StopScanCommand -> {
                mScanState
            }
            is AddressCommand -> {
                if (!mConnectStateMap.containsKey(command.address)) {
                    mConnectStateMap[command.address] = ConnectState(mActivity)
                }
                mConnectStateMap[command.address]
            }
            else -> mCurState
        }
    }

}