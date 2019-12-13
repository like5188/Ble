package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 停止广播命令
 */
class StopAdvertisingCommand : Command("停止广播命令") {

    override fun execute() {
        mReceiver?.stopAdvertising(this)
    }

}