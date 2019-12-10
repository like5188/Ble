package com.like.ble.command

import com.like.ble.receiver.IState

class InitCommand : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.init(this)
    }

}