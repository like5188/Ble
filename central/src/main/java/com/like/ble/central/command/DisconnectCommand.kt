package com.like.ble.central.command

/**
 * 断开蓝牙设备命令
 */
class DisconnectCommand(address: String) : AddressCommand("断开蓝牙设备命令", address = address) {

    override fun needExecuteImmediately(): Boolean {
        return true
    }

}