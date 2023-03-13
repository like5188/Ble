package com.like.ble.central.command

/**
 * 断开蓝牙设备命令
 */
class DisconnectCommand(
    address: String,
    timeout: Long = 10000L,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) : AddressCommand("断开蓝牙设备命令", timeout = timeout, immediately = true, address = address, onCompleted = onCompleted, onError = onError)