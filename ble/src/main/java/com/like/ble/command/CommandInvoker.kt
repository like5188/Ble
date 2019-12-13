package com.like.ble.command

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * 命令请求者。
 */
class CommandInvoker(private val mActivity: FragmentActivity) {
    private val mCommands = Channel<Command>()

    init {
        mActivity.lifecycleScope.launch {
            for (command in mCommands) {
                Log.d("CommandInvoker", "开始执行：$command")
                command.execute()
            }
        }
    }

    fun addCommand(command: Command) {
        mActivity.lifecycleScope.launch {
            mCommands.send(command)
        }
    }

}