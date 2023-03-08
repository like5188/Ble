package com.like.ble.handler

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import com.like.ble.invoker.CommandLooper
import com.like.ble.util.isBluetoothEnableAndSettingIfDisabled
import com.like.ble.util.isSupportBluetooth
import kotlinx.coroutines.launch

/**
 * 命令处理
 * 功能：
 * 1、判断蓝牙是否打开。
 * 2、提供关闭循环的方法。
 */
abstract class CommandHandler(val mActivity: ComponentActivity) {
    private val mCommandLooper: CommandLooper by lazy { CommandLooper(mActivity) }

    init {
        if (!mActivity.isSupportBluetooth()) {
            throw UnsupportedOperationException("设备不支持蓝牙")
        }
    }

    fun execute(command: Command) {
        mActivity.lifecycleScope.launch {
            if (!mActivity.isBluetoothEnableAndSettingIfDisabled()) {
                command.error("蓝牙未打开")
                return@launch
            }

            if (onExecute(command)) {
                mCommandLooper.addCommand(command)
            }
        }
    }

    fun close() {
        mCommandLooper.close()
        onClose()
    }

    /**
     * @return true：执行；false：不执行
     */
    protected abstract suspend fun onExecute(command: Command): Boolean

    protected abstract fun onClose()

}
