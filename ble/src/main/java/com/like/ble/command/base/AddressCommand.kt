package com.like.ble.command.base

import android.bluetooth.BluetoothAdapter

/**
 * 带蓝牙地址的蓝牙命令
 *
 * @param address   蓝牙设备地址
 */
abstract class AddressCommand(
    des: String,
    timeout: Long = 0L,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    val address: String
) : Command(des, timeout, onCompleted, onError) {
    init {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            failureAndCompleteIfIncomplete("invalid address：$address")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddressCommand) return false

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}