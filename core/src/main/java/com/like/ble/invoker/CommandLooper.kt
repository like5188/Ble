package com.like.ble.invoker

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.Command
import com.like.common.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/**
 * 命令循环。
 */
class CommandLooper(private val mActivity: ComponentActivity) {
    private val mCommands = Channel<Command>()
    private lateinit var mJob: Job

    init {
        initLooper()
    }

    private fun initLooper() {
        mJob = mActivity.lifecycleScope.launch {
            for (command in mCommands) {
                Logger.i("开始执行命令：$command")
                command.execute()
                do {
                    delay(20)
                } while (!command.isCompleted())
                Logger.d("命令执行完成：$command")
            }
        }
    }

    fun addCommand(command: Command) {
        mActivity.lifecycleScope.launch(Dispatchers.Main) {
            // 判断命令是否需要立即执行
            if (command.immediately) {
                mJob.cancelAndJoin()
                initLooper()
                mCommands.trySend(command)
            } else {
                mCommands.trySend(command)
            }
        }
    }

    fun close() {
        mCommands.close()
        mJob.cancel()
    }

}