package com.like.ble.central.command

/**
 * readRemoteRssi命令
 */
class ReadRemoteRssiCommand(
    address: String,
    timeout: Long = 3000L,
    onError: ((Throwable) -> Unit)? = null,
    private val onResult: ((Int) -> Unit)? = null
) : AddressCommand("readRemoteRssi命令", timeout = timeout, onError = onError, address = address) {

    override fun doOnResult(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is Int) {
                onResult?.invoke(arg0)
            }
        }
    }

    override fun needExecuteImmediately(): Boolean {
        return false
    }

}