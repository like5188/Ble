package com.like.ble.peripheral.executor

import com.like.ble.command.Command
import com.like.ble.executor.ICommandExecutor
import com.like.ble.peripheral.command.StartAdvertisingCommand
import com.like.ble.peripheral.command.StopAdvertisingCommand

/**
 * 外围设备蓝牙命令执行者。
 */
abstract class PeripheralCommandExecutor : ICommandExecutor {
    protected var startAdvertisingCommand: StartAdvertisingCommand? = null
    protected var stopAdvertisingCommand: StopAdvertisingCommand? = null
    override suspend fun execute(command: Command) {
        when (command) {
            is StartAdvertisingCommand -> {
                startAdvertisingCommand = command
                startAdvertising(command)
            }
            is StopAdvertisingCommand -> {
                stopAdvertisingCommand = command
                stopAdvertising(command)
            }
        }
    }

    open fun startAdvertising(command: StartAdvertisingCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun stopAdvertising(command: StopAdvertisingCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    override fun close() {
        startAdvertisingCommand?.clearJobs()
        stopAdvertisingCommand?.clearJobs()
        startAdvertisingCommand = null
        stopAdvertisingCommand = null
    }
}
