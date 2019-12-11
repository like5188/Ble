package com.like.ble.command

/**
 * 停止扫描命令
 */
class StopScanCommand : Command() {

    override fun execute() {
        mReceiver?.stopScan(this)
    }

}