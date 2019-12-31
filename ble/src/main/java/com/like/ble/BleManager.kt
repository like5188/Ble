package com.like.ble

import com.like.ble.command.base.Command
import com.like.ble.executor.IExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 蓝牙设备管理
 * 包括中心设备、外围设备。
 */
object BleManager {
    private var mExecutor: IExecutor? = null

    /**
     * 设置蓝牙命令执行者
     *
     * @param executor  蓝牙命令执行者。
     * 内置了两个执行者：
     * [com.like.ble.executor.CentralExecutor]：用于中心设备
     * [com.like.ble.executor.PeripheralExecutor]：用于外围设备
     */
    fun setExecutor(executor: IExecutor) {
        mExecutor = executor
    }

    /**
     * 发送命令
     *
     * @param command   蓝牙命令。
     * 内置了多个基本命令在[com.like.ble.command]包下
     */
    suspend fun sendCommand(command: Command) {
        val executor = mExecutor
        if (executor == null) {
            command.failureAndCompleteIfIncomplete("mExecutor is null")
            return
        }
        withContext(Dispatchers.IO) {
            executor.execute(command)
        }
    }

    /**
     * 释放资源
     */
    fun close() {
        mExecutor?.close()
    }

}