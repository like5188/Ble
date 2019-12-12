package com.like.ble.command

import com.like.ble.state.State

/**
 * 蓝牙相关命令的接口
 */
abstract class Command {
    var mReceiver: State? = null

    abstract fun execute()

}