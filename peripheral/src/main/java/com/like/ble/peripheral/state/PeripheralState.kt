package com.like.ble.peripheral.state

import com.like.ble.command.Command
import com.like.ble.peripheral.command.StartAdvertisingCommand
import com.like.ble.peripheral.command.StopAdvertisingCommand
import com.like.ble.state.IState

/**
 * 外围设备蓝牙状态基类。
 */
abstract class PeripheralState : IState {
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
