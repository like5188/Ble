package com.like.ble.command

import com.like.ble.receiver.IState

class WriteCommand(
    val data: ByteArray,
    val address: String,
    val characteristicUuidString: String,
    val writeTimeout: Long = 0L,
    val maxTransferSize: Int = 20,
    val onSuccess: (() -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.write(this)
    }

}