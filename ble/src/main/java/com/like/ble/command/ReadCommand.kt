package com.like.ble.command

import com.like.ble.receiver.IState
import java.nio.ByteBuffer

class ReadCommand(
    val address: String,
    val characteristicUuidString: String,
    val readTimeout: Long = 0L,
    val maxFrameTransferSize: Int = 300,
    val isWholeFrame: (ByteBuffer) -> Boolean,
    val onSuccess: ((ByteArray?) -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.read(this)
    }

}