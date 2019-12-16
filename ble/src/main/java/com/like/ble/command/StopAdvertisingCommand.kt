package com.like.ble.command

/**
 * 停止广播命令
 */
class StopAdvertisingCommand : Command("停止广播命令") {

    override fun execute() {
        mReceiver?.stopAdvertising(this)
    }

}