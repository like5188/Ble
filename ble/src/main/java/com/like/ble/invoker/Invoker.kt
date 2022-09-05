package com.like.ble.invoker

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.CloseCommand
import com.like.ble.command.DisconnectCommand
import com.like.ble.command.StopAdvertisingCommand
import com.like.ble.command.StopScanCommand
import com.like.ble.command.base.Command
import com.like.ble.utils.isBluetoothEnable
import com.like.ble.utils.isSupportBluetooth
import com.like.common.util.activityresultlauncher.requestMultiplePermissions
import com.like.common.util.activityresultlauncher.startActivityForResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 命令请求者
 */
class Invoker(private val mActivity: FragmentActivity) {
    companion object {
        private val TAG = Invoker::class.java.simpleName
    }

    private val mCommands = Channel<Command>()
    private var mCurCommand: Command? = null
    private var mCancel = AtomicBoolean(false)

    init {
        mActivity.lifecycleScope.launch {
            for (command in mCommands) {
                Log.i(TAG, "开始执行命令：$command")
                mCurCommand = command
                command.execute()
                mCancel.set(false)
                while (!command.isCompleted() && !mCancel.get()) {
                    delay(20)
                }
                if (mCancel.get()) {
                    Log.w(TAG, "取消等待命令完成：$command")
                } else {
                    Log.w(TAG, "命令执行完成：$command")
                }
                delay(100)
            }
        }
    }

    fun addCommand(command: Command) {
        mActivity.lifecycleScope.launch(Dispatchers.Main) {
            if (!mActivity.isSupportBluetooth()) {
                command.errorAndComplete("手机不支持蓝牙")
                return@launch
            }

            val checkPermissions = mActivity.requestMultiplePermissions(
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ).all { it.value }
            if (!checkPermissions) {
                command.errorAndComplete("蓝牙权限被拒绝")
                return@launch
            }

            if (!isBleOpened()) {
                command.errorAndComplete("蓝牙未打开")
                return@launch
            }

            val curCommand = mCurCommand
            // 如果相同的命令正在执行，则抛弃
            if (curCommand != null &&
                !curCommand.isCompleted() &&
                curCommand::class.java == command::class.java &&
                curCommand == command
            ) {
                Log.w(TAG, "命令正在执行，直接抛弃：$command")
                return@launch
            }
            // 判断命令是否需要立即执行
            if (command is CloseCommand || command is StopScanCommand || command is DisconnectCommand || command is StopAdvertisingCommand) {
                mCancel.set(true)
                mCommands.send(command)
                return@launch
            }
            // 排队等候前面的命令完成
            mCommands.send(command)
        }
    }

    fun close() {
        mCancel.set(true)
        mCommands.close()
        mCurCommand = null
    }

    private suspend fun isBleOpened(): Boolean = if (mActivity.isBluetoothEnable()) {
        true
    } else {// 蓝牙功能未打开
        // 弹出开启蓝牙的对话框
        val activityResult = mActivity.startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        activityResult.resultCode == Activity.RESULT_OK
    }

}