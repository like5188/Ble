package com.like.ble.executor

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import com.like.ble.invoker.CommandLooper
import com.like.ble.utils.isBleOpened
import com.like.ble.utils.isSupportBluetooth
import kotlinx.coroutines.launch

/**
 * 命令执行者
 * 功能：
 * 1、判断蓝牙操作前置条件是否满足。
 * 2、提供关闭循环的方法。
 */
abstract class CommandExecutor(val mActivity: ComponentActivity) {
    private val mCommandLooper: CommandLooper by lazy { CommandLooper(mActivity) }

    fun execute(command: Command) {
        mActivity.lifecycleScope.launch {
            if (!mActivity.isSupportBluetooth()) {
                command.errorAndComplete("手机不支持蓝牙")
                return@launch
            }

            if (!mActivity.isBleOpened()) {
                command.errorAndComplete("蓝牙未打开")
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
