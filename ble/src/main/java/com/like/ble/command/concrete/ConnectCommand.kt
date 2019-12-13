package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 连接蓝牙命令
 *
 * @param address           蓝牙设备地址
 * @param connectTimeout    连接超时时间（毫秒）
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class ConnectCommand(
    val address: String,
    val connectTimeout: Long = 20000L,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("连接蓝牙命令") {

    override fun execute() {
        mReceiver?.connect(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConnectCommand) return false

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}