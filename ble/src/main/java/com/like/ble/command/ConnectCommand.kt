package com.like.ble.command

import android.bluetooth.BluetoothGattService
import com.like.ble.command.base.AddressCommand

/**
 * 连接蓝牙设备命令
 */
class ConnectCommand(
    address: String,
    timeout: Long = 10000L,
    onError: ((Throwable) -> Unit)? = null,
    private val onResult: ((List<BluetoothGattService>) -> Unit)? = null
) : AddressCommand("连接蓝牙设备命令", timeout = timeout, onError = onError, address = address) {

    override suspend fun execute() {
        mReceiver?.connect(this)
    }

    override fun doOnResult(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is List<*>) {
                onResult?.invoke(arg0 as List<BluetoothGattService>)
            }
        }
    }
}