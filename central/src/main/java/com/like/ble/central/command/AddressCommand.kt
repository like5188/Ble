package com.like.ble.central.command

import android.bluetooth.BluetoothAdapter
import com.like.ble.command.Command

/**
 * 带蓝牙地址的蓝牙命令
 *
 * @param address   蓝牙设备地址
 */
abstract class AddressCommand(
    des: String,
    timeout: Long = 0L,
    immediately: Boolean = false,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    val address: String
) : Command(des, timeout, immediately, onCompleted, onError) {
    init {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            error("invalid address：$address")
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