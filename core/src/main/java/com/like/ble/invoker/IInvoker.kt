package com.like.ble.invoker

import com.like.ble.command.Command

/**
 * 命令请求者接口
 */
interface IInvoker {
    fun addCommand(command: Command)
    fun close()
}