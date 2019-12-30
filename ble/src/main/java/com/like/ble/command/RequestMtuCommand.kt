package com.like.ble.command

import com.like.ble.command.base.AddressCommand

/**
 * requestMtu命令
 *
 * @param mtu               需要设置的MTU值
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class RequestMtuCommand(
    address: String,
    val mtu: Int,
    timeout: Long = 3000L,
    private val onSuccess: ((Int) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : AddressCommand("requestMtu命令", timeout, address) {

    init {
        if (mtu < 23 || mtu > 517) {
            failureAndCompleteIfIncomplete("the range of mtu is [23，517]")
        }
    }

    override suspend fun execute() {
        mReceiver?.setMtu(this)
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