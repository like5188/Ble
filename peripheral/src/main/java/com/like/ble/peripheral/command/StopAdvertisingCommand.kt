package com.like.ble.peripheral.command

import com.like.ble.command.Command

/**
 * 停止广播命令
 */
class StopAdvertisingCommand(
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
) : Command("停止广播命令", immediately = true, onCompleted = onCompleted, onError = onError)