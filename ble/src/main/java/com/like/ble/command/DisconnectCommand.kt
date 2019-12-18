package com.like.ble.command

/**
 * 断开蓝牙连接命令
 *
 * @param address           蓝牙设备地址
 */
class DisconnectCommand(address: String) : Command("断开蓝牙连接命令", address) {

    override fun execute() {
        mReceiver?.disconnect(this)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_DEVICE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DisconnectCommand) return false

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}