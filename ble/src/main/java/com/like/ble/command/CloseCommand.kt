package com.like.ble.command

import com.like.ble.command.base.Command

/**
 * 释放命令执行者资源的命令
 */
internal class CloseCommand : Command("释放命令执行者资源的命令") {

    override suspend fun execute() {
        mReceiver?.close(this)
    }

}