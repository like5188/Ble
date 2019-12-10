package com.like.ble.command

import com.like.ble.receiver.IState

class StopAdvertisingCommand : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.stopAdvertising(this)
    }

}