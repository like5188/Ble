package com.like.ble.command

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.concrete.*
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
    private var mCurCommand: Command? = null

    init {
        mActivity.lifecycleScope.launch {
            for (command in mCommands) {
                Log.i(TAG, "开始执行命令：$command")
                mCurCommand = command
                command.execute()
                while (!command.isCompleted()) {
                    delay(20)
                }
                Log.w(TAG, "命令执行完成：$command")
            }
        }
    }

    fun addCommand(command: Command) {
        val curCommand = mCurCommand
        // 判断需要抛弃
        if (curCommand != null && !curCommand.isCompleted() && isSameCommand(command, curCommand)) {
            Log.w(TAG, "命令正在执行，直接抛弃：$command")
            return
        }
        // 判断排队

        // 判断立即执行

        sendCommand(command)
    }

    /**
     * 判断是否是同一命令。
     *
     * 其中[InitCommand]、[StartAdvertisingCommand]、[StopAdvertisingCommand]、[StartScanCommand]、[StopScanCommand]、[CloseCommand]是同一类型，则判断为同一命令
     * 其中[ConnectCommand]、[DisconnectCommand]、[ReadCharacteristicCommand]、[WriteCharacteristicCommand]、[SetMtuCommand]是同一类型，并且内容相同，才判断为同一命令
     */
    private fun isSameCommand(command1: Command, command2: Command): Boolean {
        if (command1::class.java == command2::class.java) {
            when (command1) {
                is ConnectCommand, is DisconnectCommand, is ReadCharacteristicCommand, is WriteCharacteristicCommand, is SetMtuCommand -> {
                    if (command1 == command2) {// 必须比较内容
                        return true
                    }
                }
                else -> {
                    return true
                }
            }
        }
        return false
    }

    private fun sendCommand(command: Command) {
        mActivity.lifecycleScope.launch {
            mCommands.send(command)
        }
    }

    fun close() {
        mCommands.close()
    }

}