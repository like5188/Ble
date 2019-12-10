package com.like.ble.command

import com.like.ble.receiver.IState

class ConnectCommand(
    val address: String,
    val connectTimeout: Long = 20000L,
    val onSuccess: (() -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.connect(this)
    }

}