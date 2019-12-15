package com.like.ble.command

import com.like.ble.command.Command

/**
 * 停止扫描蓝牙设备命令
 */
class StopScanCommand : Command("停止扫描蓝牙设备命令") {

    override fun execute() {
        mReceiver?.stopScan(this)
    }

}