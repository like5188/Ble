package com.like.ble.command

/**
 * 停止扫描蓝牙设备命令
 */
class StopScanCommand : Command("停止扫描蓝牙设备命令") {

    override suspend fun execute() {
        mReceiver?.stopScan(this)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_SCAN

}