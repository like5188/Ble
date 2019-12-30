package com.like.ble.executor

import com.like.ble.command.base.Command

/**
 * 命令真正的执行者
 */
interface IExecutor {
    suspend fun execute(command: Command)
    fun close()
}