package com.like.ble.command

import com.like.ble.receiver.IState

class CloseCommand : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.close(this)
    }

}