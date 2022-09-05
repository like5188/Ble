package com.like.ble.central.state

import com.like.ble.central.command.*
import com.like.ble.command.CloseCommand
import com.like.ble.command.Command
import com.like.ble.state.IState

/**
 * 中心设备蓝牙状态基类。
 */
abstract class CentralState : IState {

    override fun close(command: CloseCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    override suspend fun execute(command: Command) {
        when (command) {
            is StartScanCommand -> startScan(command)
            is StopScanCommand -> stopScan(command)
            is ConnectCommand -> connect(command)
            is DisconnectCommand -> disconnect(command)
            is ReadCharacteristicCommand -> readCharacteristic(command)
            is WriteCharacteristicCommand -> writeCharacteristic(command)
            is ReadDescriptorCommand -> readDescriptor(command)
            is WriteDescriptorCommand -> writeDescriptor(command)
            is RequestMtuCommand -> requestMtu(command)
            is SetCharacteristicNotificationCommand -> setCharacteristicNotification(command)
            is ReadNotifyCommand -> readNotify(command)
            is ReadRemoteRssiCommand -> readRemoteRssi(command)
            is RequestConnectionPriorityCommand -> requestConnectionPriority(command)
        }
    }

    open fun startScan(command: StartScanCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun stopScan(command: StopScanCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun connect(command: ConnectCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun disconnect(command: DisconnectCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun readCharacteristic(command: ReadCharacteristicCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun writeCharacteristic(command: WriteCharacteristicCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun readDescriptor(command: ReadDescriptorCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun writeDescriptor(command: WriteDescriptorCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun requestMtu(command: RequestMtuCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun setCharacteristicNotification(command: SetCharacteristicNotificationCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun readNotify(command: ReadNotifyCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun readRemoteRssi(command: ReadRemoteRssiCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

}
