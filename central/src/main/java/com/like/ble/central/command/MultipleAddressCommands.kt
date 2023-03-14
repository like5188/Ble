package com.like.ble.central.command

import com.like.ble.command.Command
import kotlinx.coroutines.delay

/**
 * 组合命令
 *
 * 顺序执行添加的[AddressCommand]命令。
 * 在前一个命令完成后再执行下一个命令，否则会阻塞（除了回调命令，它就算放在前面也不会阻塞）。
 * sample中的组合命令：
 *  1、[ReadNotifyCommand]+[WriteCharacteristicCommand]：发送命令并接收通知数据，注意必须要开启通知才能接收数据。
 */
class MultipleAddressCommands : Command("组合命令") {
    private val mCommands = mutableListOf<AddressCommand>()
    private var mCallbackCommand: AddressCommand? = null

    /**
     * @param isCallbackCommand     是否需要在这个命令上执行回调
     */
    fun addCommand(command: AddressCommand, isCallbackCommand: Boolean = false) {
        if (isCallbackCommand) {
            mCallbackCommand = command
        }
        mCommands.add(command)
    }

    override suspend fun execute() {
        if (mCommands.isEmpty() || mCommands.size == 1) {
            error("${des}至少需要添加2个命令")
            return
        }
        mCommands.forEach { command ->
            if (command == mCallbackCommand) {
                command.setInterceptor(object : Interceptor {
                    override fun interceptCompleted(command: Command) {
                        mCallbackCommand?.onCompleted?.invoke()
                        complete()
                    }

                    override fun interceptFailure(command: Command, throwable: Throwable) {
                        mCallbackCommand?.onError?.invoke(throwable)
                        error(throwable.message ?: "unknown error")
                    }

                    override fun interceptResult(command: Command, vararg args: Any?) {
                        mCallbackCommand?.onResult(*args)
                        complete()
                    }
                })
            } else {
                command.setInterceptor(object : Interceptor {
                    override fun interceptCompleted(command: Command) {
                    }

                    override fun interceptFailure(command: Command, throwable: Throwable) {
                        mCallbackCommand?.error(throwable.message ?: "unknown error")
                        error(throwable.message ?: "unknown error")
                    }

                    override fun interceptResult(command: Command, vararg args: Any?) {
                    }
                })
            }
            command.mCommandExecutor = mCommandExecutor
            command.execute()// 直接执行，避免放入队列中造成阻塞
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
        if (other !is MultipleAddressCommands) return false
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