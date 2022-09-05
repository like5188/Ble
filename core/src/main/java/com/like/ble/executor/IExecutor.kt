package com.like.ble.executor

import com.like.ble.command.Command

/**
 * 命令真正的执行者
 */
interface IExecutor {
    fun execute(command: Command)
    fun close()
}