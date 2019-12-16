package com.like.ble.command

/**
 * requestConnectionPriority命令
 *
 * @param address               蓝牙设备地址
 * @param connectionPriority    需要设置的priority。[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]
 * @param onSuccess             命令执行成功回调
 * @param onFailure             命令执行失败回调
 */
class RequestConnectionPriorityCommand(
    val address: String,
    val connectionPriority: Int,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("requestConnectionPriority命令") {

    override fun execute() {
        mReceiver?.requestConnectionPriority(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RequestConnectionPriorityCommand) return false

        if (address != other.address) return false
        if (connectionPriority != other.connectionPriority) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + connectionPriority
        return result
    }

}