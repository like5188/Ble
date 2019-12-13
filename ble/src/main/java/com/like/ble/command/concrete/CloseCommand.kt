package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 关闭蓝牙命令
 */
class CloseCommand : Command("关闭蓝牙命令") {

    override fun execute() {
        mReceiver?.close(this)
    }

}