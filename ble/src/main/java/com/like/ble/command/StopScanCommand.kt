package com.like.ble.command

import com.like.ble.command.base.Command

/**
 * 停止扫描蓝牙设备命令
 */
class StopScanCommand : Command("停止扫描蓝牙设备命令") {

    override suspend fun execute() {
        mReceiver?.stopScan(this)
    }

}