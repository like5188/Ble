package com.like.ble.command

import com.like.ble.command.base.AddressCommand

/**
 * readRemoteRssi命令
 */
class ReadRemoteRssiCommand(
    address: String,
    timeout: Long = 3000L,
    callback: Callback? = null
) : AddressCommand("readRemoteRssi命令", timeout, callback, address) {

    override suspend fun execute() {
        mReceiver?.readRemoteRssi(this)
    }

}