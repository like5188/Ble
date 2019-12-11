package com.like.ble.command

/**
 * 停止广播命令
 */
class StopAdvertisingCommand : Command() {

    override fun execute() {
        mReceiver?.stopAdvertising(this)
    }

}