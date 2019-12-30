package com.like.ble.command

import android.bluetooth.BluetoothGattService
import com.like.ble.command.base.AddressCommand

/**
 * 连接蓝牙命令
 *
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class ConnectCommand(
    address: String,
    timeout: Long = 10000L,
    private val onSuccess: ((List<BluetoothGattService>) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : AddressCommand("连接蓝牙命令", timeout, address) {

    override suspend fun execute() {
        mReceiver?.connect(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            onSuccess?.invoke(arg0 as List<BluetoothGattService>)
        }
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

}