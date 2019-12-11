package com.like.ble.command

/**
 * 命令请求者
 */
class CommandInvoker {
    private val mCommands = mutableListOf<Command>()

    fun addCommand(command: Command) {
        mCommands.add(command)
    }

    fun execute() {
        if (mCommands.isEmpty()) return
        val listIterator = mCommands.listIterator()
        while (listIterator.hasNext()) {
            val command = listIterator.next()
            command.execute()
            listIterator.remove()
        }
    }

}