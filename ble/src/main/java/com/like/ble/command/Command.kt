package com.like.ble.command

import com.like.ble.state.IState

/**
 * 蓝牙相关命令的接口
 */
abstract class Command {
    var mReceiver: IState? = null

    abstract fun execute()

}