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

/**
 * 蓝牙中心设备相关命令处理。
 */
class CentralCommandHandler(activity: ComponentActivity) : CommandHandler(activity) {
    private var mCurCommandExecutor: ICommandExecutor? = null
    private val mScanCommandExecutor: ICommandExecutor by lazy { ScanCommandExecutor(mActivity) }

    // key：地址；value：ICommandExecutor；
    private val mConnectCommandExecutorMap = mutableMapOf<String, ICommandExecutor>()

    override suspend fun onExecute(command: Command): Boolean {
        if (!checkPermissions(mActivity, command)) {
            command.error("蓝牙权限被拒绝")
            return false
        }

        val commandExecutor = getCommandExecutorBy(command)
        if (commandExecutor == null) {
            command.error("获取 CommandExecutor 失败，无法执行命令：$command")
            return false
        }

        mCurCommandExecutor = commandExecutor
        command.mCommandExecutor = commandExecutor
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

    override fun onCloseScan() {
        mScanCommandExecutor.close()
    }

    override fun onCloseConnect(address: String) {
        if (address.isNotEmpty() && mConnectCommandExecutorMap.containsKey(address)) {
            mConnectCommandExecutorMap[address]?.close()
            mConnectCommandExecutorMap.remove(address)
        }
    }

    private fun getCommandExecutorBy(command: Command): ICommandExecutor? {
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

    private suspend fun checkPermissions(activity: ComponentActivity, command: Command): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            when (command) {
                is StartScanCommand, is StopScanCommand -> {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
                else -> {
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
                }
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