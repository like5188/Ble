package com.like.ble.command

import com.like.ble.command.base.AddressCommand

/**
 * 断开蓝牙设备命令
 */
class DisconnectCommand(address: String) : AddressCommand("断开蓝牙设备命令", address = address) {

    override suspend fun execute() {
        mReceiver?.disconnect(this)
    }

}