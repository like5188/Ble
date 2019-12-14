package com.like.ble.command

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.concrete.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 命令请求者。
 */
class CommandInvoker(private val mActivity: FragmentActivity) {
    companion object {
        private val TAG = CommandInvoker::class.java.simpleName
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
            }
        }
    }

    fun addCommand(command: Command) {
        val curCommand = mCurCommand
        // 判断需要抛弃
        if (curCommand != null && !curCommand.isCompleted() && isSameCommand(curCommand, command)) {
            Log.w(TAG, "命令正在执行，直接抛弃：$command")
            return
        }
        // 判断立即执行
        if (curCommand != null && needExecuteImmediately(curCommand, command)) {
            mCancel.set(true)
            sendCommand(command)
            return
        }
        // 排队等候
        sendCommand(command)
    }

    /**
     * 是否需要立即执行
     */
    private fun needExecuteImmediately(curCommand: Command, command: Command): Boolean {
        if (command is CloseCommand) {
            return true
        }
        if (curCommand is StartAdvertisingCommand && command is StopAdvertisingCommand) {
            return true
        }
        if (curCommand is StartScanCommand && command is StopScanCommand) {
            return true
        }
        if (curCommand is ConnectCommand ||
            curCommand is ReadCharacteristicCommand ||
            curCommand is WriteCharacteristicCommand ||
            curCommand is SetMtuCommand
        ) {
            if (command is DisconnectCommand) {
                return true
            }
        }
        return false
    }

    /**
     * 判断是否是同一命令。
     *
     * 其中[StartAdvertisingCommand]、[StopAdvertisingCommand]、[StartScanCommand]、[StopScanCommand]、[CloseCommand]是同一类型，则判断为同一命令
     * 其中[ConnectCommand]、[DisconnectCommand]、[ReadCharacteristicCommand]、[WriteCharacteristicCommand]、[SetMtuCommand]是同一类型，并且内容相同，才判断为同一命令
     */
    private fun isSameCommand(curCommand: Command, command: Command): Boolean {
        if (curCommand::class.java == command::class.java) {
            when (curCommand) {
                is ConnectCommand, is DisconnectCommand, is ReadCharacteristicCommand, is WriteCharacteristicCommand, is SetMtuCommand -> {
                    if (curCommand == command) {// 必须比较内容
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