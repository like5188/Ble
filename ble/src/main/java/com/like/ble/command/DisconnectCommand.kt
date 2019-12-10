package com.like.ble.command

import com.like.ble.receiver.IState

class DisconnectCommand(
    val address: String,
    val onSuccess: (() -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.disconnect(this)
    }

}