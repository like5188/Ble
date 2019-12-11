package com.like.ble.command

/**
 * 关闭蓝牙命令
 */
class CloseCommand : Command() {

    override fun execute() {
        mReceiver?.close(this)
    }

}