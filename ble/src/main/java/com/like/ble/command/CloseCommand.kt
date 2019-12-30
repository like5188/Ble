package com.like.ble.command

import com.like.ble.command.base.Command

/**
 * 关闭蓝牙命令
 */
class CloseCommand : Command("关闭蓝牙命令") {

    override suspend fun execute() {
        mReceiver?.close(this)
    }

}