package com.like.ble.state

import com.like.ble.command.*

/**
 * 蓝牙状态基类。
 */
abstract class State {
    open fun startAdvertising(command: StartAdvertisingCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }

    open fun stopAdvertising(command: StopAdvertisingCommand) {
        throw UnsupportedOperationException("Unsupported command")
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

    open fun setMtu(command: RequestMtuCommand) {
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

    open fun close(command: CloseCommand) {
        throw UnsupportedOperationException("Unsupported command")
    }
}