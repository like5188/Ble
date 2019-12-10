package com.like.ble.command

import com.like.ble.receiver.IState

/**
 * 停止广播命令
 */
class StopAdvertisingCommand : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.stopAdvertising(this)
    }

}