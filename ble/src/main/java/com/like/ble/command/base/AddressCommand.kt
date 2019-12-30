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
    onSuccess: BleResult? = null,
    onFailure: ((Throwable) -> Unit)? = null,
    val address: String
) : ResultCommand(des, timeout, onSuccess, onFailure) {
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