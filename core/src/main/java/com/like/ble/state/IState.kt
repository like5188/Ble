package com.like.ble.state

import com.like.ble.command.Command

/**
 * 蓝牙状态接口。
 */
interface IState {
    /**
     * 释放资源
     */
    fun close()

    /**
     * 执行命令
     */
    suspend fun execute(command: Command)

}
