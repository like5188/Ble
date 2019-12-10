package com.like.ble.command

import com.like.ble.state.IState

/**
 * 关闭蓝牙命令
 */
class CloseCommand : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.close(this)
    }

}