package com.like.ble.invoker

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 循环从命令队列中取出命令执行。
 */
class CommandLooper(private val mActivity: ComponentActivity) {
    companion object {
        private val TAG = CommandLooper::class.java.simpleName
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
            if (command.immediately) {
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

}