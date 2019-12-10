package com.like.ble.command

import com.like.ble.receiver.IState

/**
 * 停止扫描命令
 */
class StopScanCommand : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.stopScan(this)
    }

}