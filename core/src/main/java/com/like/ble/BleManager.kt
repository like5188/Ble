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
     * 释放所有资源
     */
    fun close() {
        mCommandHandler.close()
    }

    /**
     * 释放扫描的资源
     */
    fun closeScan() {
        mCommandHandler.closeScan()
    }

    /**
     * 释放指定连接的资源
     */
    fun closeConnect(address: String) {
        mCommandHandler.closeConnect(address)
    }

}
