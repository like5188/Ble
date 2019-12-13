package com.like.ble.command

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 命令请求者。
 */
class CommandInvoker(private val mActivity: FragmentActivity) {
    companion object {
        private val TAG = CommandInvoker::class.java.simpleName
    }

    private val mCommands = Channel<Command>()

    init {
        mActivity.lifecycleScope.launch {
            for (command in mCommands) {
                Log.i(TAG, "开始执行命令：$command")
                command.execute()
                while (!command.isCompleted()) {
                    delay(20)
                }
                Log.w(TAG, "命令执行完成：$command")
            }
        }
    }

    fun addCommand(command: Command) {
        mActivity.lifecycleScope.launch {
            mCommands.send(command)
        }
    }

}