package com.like.ble.invoker

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.command.*
import com.like.ble.command.base.Command
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙中心设备命令请求者。
 */
class CentralInvoker(activity: FragmentActivity) : Invoker(activity) {
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
                delay(100)
            }
        }
    }

    override fun execute(command: Command) {
        val curCommand = mCurCommand
        // 判断需要抛弃
        if (curCommand != null && !curCommand.isCompleted() && curCommand::class.java == command::class.java && curCommand == command) {
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
        if (command is CloseCommand || command is StopScanCommand || command is DisconnectCommand) {
            return true
        }
        if (curCommand is ReadNotifyCommand && command is WriteCharacteristicCommand) {// 用于发送命令，并从通知获取返回数据
            return true
        }
        return false
    }

    private fun sendCommand(command: Command) {
        mActivity.lifecycleScope.launch {
            try {
                mCommands.send(command)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun close() {
        mCommands.close()
        mCurCommand = null
    }

}