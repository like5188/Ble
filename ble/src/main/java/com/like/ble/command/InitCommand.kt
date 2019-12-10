package com.like.ble.command

import com.like.ble.receiver.IState

/**
 * 初始化蓝牙命令
 */
class InitCommand : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.init(this)
    }

}