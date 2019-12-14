package com.like.ble.invoker

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.like.ble.command.Command
import kotlinx.coroutines.delay

/**
 * 蓝牙外围设备命令请求者。
 */
class PeripheralInvoker(activity: FragmentActivity) : Invoker(activity) {
    companion object {
        private val TAG = PeripheralInvoker::class.java.simpleName
    }

    private var mCurCommand: Command? = null

    override suspend fun execute(command: Command) {
        val curCommand = mCurCommand
        // 判断需要抛弃
        if (curCommand != null && !curCommand.isCompleted() && curCommand::class.java == command::class.java) {
            Log.w(TAG, "命令正在执行，直接抛弃：$command")
            return
        }
        Log.i(TAG, "开始执行命令：$command")
        mCurCommand = command
        command.execute()
        while (!command.isCompleted()) {
            delay(20)
        }
        Log.w(TAG, "命令执行完成：$command")
    }

    override fun close() {
        mCurCommand = null
    }

}