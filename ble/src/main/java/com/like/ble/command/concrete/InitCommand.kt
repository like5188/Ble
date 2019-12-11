package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 初始化蓝牙命令
 */
class InitCommand : Command() {

    override fun execute() {
        mReceiver?.init(this)
    }

}