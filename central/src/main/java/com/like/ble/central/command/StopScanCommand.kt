package com.like.ble.central.command

import com.like.ble.command.Command

/**
 * 停止扫描蓝牙设备命令
 */
class StopScanCommand(
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
) : Command("停止扫描蓝牙设备命令", immediately = true, onCompleted = onCompleted, onError = onError)