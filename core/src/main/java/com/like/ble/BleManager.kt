package com.like.ble

import com.like.ble.command.Command
import com.like.ble.executor.IExecutor

/**
 * 蓝牙设备管理
 * 包括中心设备、外围设备。
 *
 * @param mExecutor  蓝牙命令执行者。
 */
class BleManager(private val mExecutor: IExecutor) {

    /**
     * 发送命令
     *
     * @param command   蓝牙命令。
     */
    fun sendCommand(command: Command) {
        mExecutor.execute(command)
    }

    /**
     * 释放资源
     */
    fun close() {
        mExecutor.close()
    }

}