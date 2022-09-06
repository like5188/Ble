package com.like.ble.central.command

/**
 * requestMtu命令
 *
 * @param mtu               需要设置的MTU值
 */
class RequestMtuCommand(
    address: String,
    val mtu: Int,
    timeout: Long = 3000L,
    onError: ((Throwable) -> Unit)? = null,
    private val onResult: ((Int) -> Unit)? = null
) : AddressCommand("requestMtu命令", timeout = timeout, onError = onError, address = address) {

    init {
        if (mtu < 23 || mtu > 517) {
            errorAndComplete("the range of mtu is [23，517]")
        }
    }

    override fun onResult(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is Int) {
                onResult?.invoke(arg0)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RequestMtuCommand) return false
        if (!super.equals(other)) return false

        if (mtu != other.mtu) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mtu
        return result
    }

}