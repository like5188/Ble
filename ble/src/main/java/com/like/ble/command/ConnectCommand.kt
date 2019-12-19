package com.like.ble.command

/**
 * 连接蓝牙命令
 *
 * @param address           蓝牙设备地址
 * @param timeout           命令执行超时时间（毫秒）
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class ConnectCommand(
    address: String,
    val timeout: Long = 10000L,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("连接蓝牙命令", address) {

    override fun execute() {
        mReceiver?.connect(this)
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
        if (other !is ConnectCommand) return false

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}