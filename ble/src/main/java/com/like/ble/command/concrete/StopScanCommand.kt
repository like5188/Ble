package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 停止扫描命令
 */
class StopScanCommand : Command() {

    override fun execute() {
        mReceiver.stopScan(this)
    }

}