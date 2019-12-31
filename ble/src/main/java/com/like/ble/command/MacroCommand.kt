package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import com.like.ble.command.base.Command
import kotlinx.coroutines.delay

/**
 * 顺序执行添加的[AddressCommand]命令，且在前一个成功后再执行下一个命令，否则会阻塞。
 */
class MacroCommand : Command("宏命令") {
    private val mCommands = mutableListOf<AddressCommand>()
    private var mCallbackCommand: AddressCommand? = null

    fun addCommand(command: AddressCommand, isCallbackCommand: Boolean = false) {
        if (isCallbackCommand) {
            mCallbackCommand = command
        }
        mCommands.add(command)
    }

    override suspend fun execute() {
        if (mCommands.isEmpty() || mCommands.size == 1) {
            errorAndComplete("宏命令至少需要2个命令")
            return
        }
        mCommands.forEach { command ->
            if (command == mCallbackCommand) {
                command.addInterceptor(object : Interceptor {
                    override fun interceptCompleted(command: Command) {
                        mCallbackCommand?.onCompleted?.invoke()
                        complete()
                    }

                    override fun interceptFailure(command: Command, throwable: Throwable) {
                        mCallbackCommand?.onError?.invoke(throwable)
                        errorAndComplete(throwable.message ?: "unknown error")
                    }

                    override fun interceptResult(command: Command, vararg args: Any?) {
                        mCallbackCommand?.doOnResult(*args)
                        complete()
                    }
                })
            } else {
                command.addInterceptor(object : Interceptor {
                    override fun interceptCompleted(command: Command) {
                    }

                    override fun interceptFailure(command: Command, throwable: Throwable) {
                        mCallbackCommand?.errorAndComplete(throwable.message ?: "unknown error")
                        errorAndComplete(throwable.message ?: "unknown error")
                    }

                    override fun interceptResult(command: Command, vararg args: Any?) {
                    }
                })
            }
            command.mReceiver = mReceiver
            command.execute()
            while (command != mCallbackCommand && !command.isCompleted()) {
                delay(20)
            }
            if (command.isError()) {
                return
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MacroCommand) return false
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