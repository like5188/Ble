package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 设置MTU命令
 *
 * @param address           蓝牙设备地址
 * @param mtu               需要设置的MTU值
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class SetMtuCommand(
    val address: String,
    val mtu: Int,
    private val onSuccess: ((Int) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("设置MTU命令") {

    override fun execute() {
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
        if (other !is SetMtuCommand) return false

        if (address != other.address) return false
        if (mtu != other.mtu) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + mtu
        return result
    }

}