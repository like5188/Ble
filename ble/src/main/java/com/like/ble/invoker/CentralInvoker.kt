package com.like.ble.invoker

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙中心设备命令请求者。
 */
class CentralInvoker(private val mActivity: FragmentActivity) : Invoker(mActivity) {
    companion object {
        private val TAG = CentralInvoker::class.java.simpleName
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

    override suspend fun execute(command: Command) {
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
        if (command.hasGroup(Command.GROUP_CLOSE)) {
            return true
        }
        if (curCommand.hasGroup(Command.GROUP_CENTRAL_SCAN) && command is StopScanCommand) {
            return true
        }
        if (curCommand.hasGroup(Command.GROUP_CENTRAL_DEVICE) && command is DisconnectCommand) {
            return true
        }
        if (curCommand is ReadNotifyCommand && command is WriteCharacteristicCommand) {
            return true
        }
        return false
    }

    /**
     * 判断是否是同一命令。
     *
     * 其中与指定设备无关的命令
     * [StartScanCommand]、
     * [StopScanCommand]、
     * [CloseCommand]
     * 判断是同一类型，则为同一命令
     *
     * 其中与指定设备有关的命令，必须判断同一类型，并且内容相同，才为同一命令
     */
    private fun isSameCommand(curCommand: Command, command: Command): Boolean {
        if (curCommand::class.java == command::class.java) {
            when {
                curCommand.hasGroup(Command.GROUP_CENTRAL_SCAN) || command.hasGroup(Command.GROUP_CLOSE) -> {
                    return true
                }
                else -> {
                    if (curCommand == command) {// 必须比较内容
                        return true
                    }
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

    override fun close() {
        mCommands.close()
        mCurCommand = null
    }

}