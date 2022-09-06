package com.like.ble.invoker

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import com.like.ble.utils.checkPermissions
import com.like.ble.utils.isBleOpened
import com.like.ble.utils.isSupportBluetooth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 命令请求者
 * 功能：
 * 1、添加命令，并判断执行条件是否满足。
 * 2、循环从命令队列中取出命令执行。并提供关闭循环的方法。
 */
class Invoker(private val mActivity: ComponentActivity) : IInvoker {
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

    override fun addCommand(command: Command) {
        mActivity.lifecycleScope.launch(Dispatchers.Main) {
            if (!mActivity.isSupportBluetooth()) {
                command.errorAndComplete("手机不支持蓝牙")
                return@launch
            }

            if (!mActivity.checkPermissions()) {
                command.errorAndComplete("蓝牙权限被拒绝")
                return@launch
            }

            if (!mActivity.isBleOpened()) {
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
            if (command.needExecuteImmediately()) {
                mCancel.set(true)
                mCommands.send(command)
                return@launch
            }
            // 排队等候前面的命令完成
            mCommands.send(command)
        }
    }

    override fun close() {
        mCancel.set(true)
        mCommands.close()
        mCurCommand = null
    }

}