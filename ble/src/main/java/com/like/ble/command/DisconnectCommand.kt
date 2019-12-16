package com.like.ble.command

/**
 * 断开蓝牙连接命令
 *
 * @param address           蓝牙设备地址
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class DisconnectCommand(
    address: String,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("断开蓝牙连接命令", address) {

    override fun execute() {
        mReceiver?.disconnect(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_DEVICE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DisconnectCommand) return false

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}