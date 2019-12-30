package com.like.ble.command

import com.like.ble.command.base.Command

/**
 * 停止广播命令
 */
class StopAdvertisingCommand : Command("停止广播命令") {

    override suspend fun execute() {
        mReceiver?.stopAdvertising(this)
    }

}