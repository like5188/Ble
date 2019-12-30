package com.like.ble.command

import com.like.ble.command.base.AddressCommand

/**
 * readRemoteRssi命令
 *
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadRemoteRssiCommand(
    address: String,
    timeout: Long = 3000L,
    private val onSuccess: ((Int) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : AddressCommand("readRemoteRssi命令", timeout, address) {

    override suspend fun execute() {
        mReceiver?.readRemoteRssi(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is Int) {
                onSuccess?.invoke(arg0)
            }
        }
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

}