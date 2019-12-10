package com.like.ble.command

import com.like.ble.receiver.IState

class SetMtuCommand(
    val address: String,
    val mtu: Int,
    val onSuccess: ((Int) -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.setMtu(this)
    }

}