package com.like.ble.central.command

/**
 * 断开蓝牙设备命令
 */
class DisconnectCommand(
    address: String,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) : AddressCommand("断开蓝牙设备命令", immediately = true, address = address, onCompleted = onCompleted, onError = onError)