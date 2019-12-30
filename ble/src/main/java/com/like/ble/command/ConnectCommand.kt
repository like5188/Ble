package com.like.ble.command

import com.like.ble.command.base.AddressCommand

/**
 * 连接蓝牙命令
 */
class ConnectCommand(
    address: String,
    timeout: Long = 10000L,
    callback: Callback? = null
) : AddressCommand("连接蓝牙命令", timeout, callback, address) {

    override suspend fun execute() {
        mReceiver?.connect(this)
    }

}