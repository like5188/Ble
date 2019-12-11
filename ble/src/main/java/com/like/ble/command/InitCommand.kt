package com.like.ble.command

/**
 * 初始化蓝牙命令
 */
class InitCommand : Command() {

    override fun execute() {
        mReceiver?.init(this)
    }

}