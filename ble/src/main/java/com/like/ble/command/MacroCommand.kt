package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import com.like.ble.command.base.Command
import kotlinx.coroutines.delay

/**
 * 顺序执行添加的[AddressCommand]命令，且在前一个成功后再执行下一个命令，否则会阻塞。
 */
class MacroAddressCommand : Command("宏命令") {
    private val mCommands = mutableListOf<AddressCommand>()
    private var mCallbackCommand: AddressCommand? = null

    @Throws(UnsupportedOperationException::class, IllegalArgumentException::class)
    fun addCommand(command: AddressCommand, isCallbackCommand: Boolean = false) {
        if (isCallbackCommand) {
            mCallbackCommand = command
        }
        mCommands.add(command)
    }

    override suspend fun execute() {
        if (mCommands.isEmpty()) return
        if (mCommands.size == 1) {
            val command = mCommands[0]
            command.mReceiver = mReceiver
            command.execute()
            return
        }
        mCommands.forEach { command ->
            if (command != mCallbackCommand) {
                command.addInterceptor(object : Command.Interceptor {
                    override fun interceptCompleted(command: Command) {
                    }

                    override fun interceptFailure(command: Command, throwable: Throwable) {
                        mCallbackCommand?.callback?.onFailure(throwable)
                    }

                    override fun interceptResult(command: Command, vararg args: Any?) {
                    }
                })
            }
            command.mReceiver = mReceiver
            command.execute()
            while (!command.isCompleted()) {
                delay(20)
            }
            if (!command.isSuccess() && command != mCallbackCommand) {
                return
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MacroAddressCommand) return false
        if (!super.equals(other)) return false

        if (mCommands != other.mCommands) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mCommands.hashCode()
        return result
    }

}