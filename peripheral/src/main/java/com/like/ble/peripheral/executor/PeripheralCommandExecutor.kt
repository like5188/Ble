package com.like.ble.peripheral.executor

import com.like.ble.command.Command
import com.like.ble.peripheral.command.StartAdvertisingCommand
import com.like.ble.peripheral.command.StopAdvertisingCommand
import com.like.ble.executor.ICommandExecutor

/**
 * 外围设备蓝牙命令执行者。
 */
abstract class PeripheralCommandExecutor : ICommandExecutor {
    override suspend fun execute(command: Command) {
        when (command) {
            is StartAdvertisingCommand -> startAdvertising(command)
            is StopAdvertisingCommand -> stopAdvertising(command)
        }
    }

    open fun startAdvertising(command: StartAdvertisingCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun stopAdvertising(command: StopAdvertisingCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

}
