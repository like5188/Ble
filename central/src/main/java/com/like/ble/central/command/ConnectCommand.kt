package com.like.ble.central.command

import android.bluetooth.BluetoothGattService

/**
 * 连接蓝牙设备命令
 */
class ConnectCommand(
    address: String,
    timeout: Long = 10000L,
    onError: ((Throwable) -> Unit)? = null,
    private val onResult: ((List<BluetoothGattService>) -> Unit)? = null
) : AddressCommand("连接蓝牙设备命令", timeout = timeout, onError = onError, address = address) {

    override fun doOnResult(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is List<*>) {
                onResult?.invoke(arg0 as List<BluetoothGattService>)
            }
        }
    }

    override fun needExecuteImmediately(): Boolean {
        return false
    }
}