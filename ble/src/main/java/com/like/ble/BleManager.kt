package com.like.ble

import com.like.ble.command.base.Command
import com.like.ble.executor.IExecutor

/**
 * 蓝牙设备管理
 * 包括中心设备、外围设备。
 *
 * @param mExecutor  蓝牙命令执行者。
 * 内置了两个执行者，位于[com.like.ble.executor]包中：
 * [com.like.ble.executor.CentralExecutor]：用于中心设备
 * [com.like.ble.executor.PeripheralExecutor]：用于外围设备
 */
class BleManager(private val mExecutor: IExecutor) {

    /**
     * 发送命令
     *
     * @param command   蓝牙命令。
     * 内置了多个基本命令，位于[com.like.ble.command]包中
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