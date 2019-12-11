package com.like.ble.invoker

import com.like.ble.command.Command

class Invoker {
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