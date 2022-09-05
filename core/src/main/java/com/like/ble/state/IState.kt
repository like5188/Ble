package com.like.ble.state

import com.like.ble.command.CloseCommand
import com.like.ble.command.Command

/**
 * 蓝牙状态接口。
 */
interface IState {
    fun close(command: CloseCommand)
    suspend fun execute(command: Command)
}