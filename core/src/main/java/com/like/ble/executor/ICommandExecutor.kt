package com.like.ble.executor

import com.like.ble.command.Command

/**
 * 蓝牙命令真正的执行者。
 */
interface ICommandExecutor {
    /**
     * 释放资源
     */
    fun close()

    /**
     * 执行命令
     */
    suspend fun execute(command: Command)

}
