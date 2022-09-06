package com.like.ble

import com.like.ble.command.Command
import com.like.ble.handler.CommandHandler

/**
 * 蓝牙设备管理入口
 * 包括中心设备、外围设备。
 */
class BleManager(private val mCommandHandler: CommandHandler) {

    /**
     * 发送命令
     *
     * @param command   蓝牙命令。
     */
    fun sendCommand(command: Command) {
        mCommandHandler.execute(command)
    }

    /**
     * 释放资源
     */
    fun close() {
        mCommandHandler.close()
    }

}
