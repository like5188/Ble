package com.like.ble.central.executor

import com.like.ble.central.command.*
import com.like.ble.command.Command
import com.like.ble.executor.ICommandExecutor

/**
 * 中心设备蓝牙命令执行者。
 */
abstract class CentralCommandExecutor : ICommandExecutor {
    protected var startScanCommand: StartScanCommand? = null
    protected var stopScanCommand: StopScanCommand? = null
    protected var connectCommand: ConnectCommand? = null
    protected var disconnectCommand: DisconnectCommand? = null
    protected var readCharacteristicCommand: ReadCharacteristicCommand? = null
    protected var writeCharacteristicCommand: WriteCharacteristicCommand? = null
    protected var readDescriptorCommand: ReadDescriptorCommand? = null
    protected var writeDescriptorCommand: WriteDescriptorCommand? = null
    protected var requestMtuCommand: RequestMtuCommand? = null
    protected var setCharacteristicNotificationCommand: SetCharacteristicNotificationCommand? = null
    protected var readNotifyCommand: ReadNotifyCommand? = null
    protected var readRemoteRssiCommand: ReadRemoteRssiCommand? = null
    protected var requestConnectionPriorityCommand: RequestConnectionPriorityCommand? = null

    override suspend fun execute(command: Command) {
        when (command) {
            is StartScanCommand -> {
                startScanCommand = command
                startScan(command)
            }
            is StopScanCommand -> {
                stopScanCommand = command
                stopScan(command)
            }
            is ConnectCommand -> {
                connectCommand = command
                connect(command)
            }
            is DisconnectCommand -> {
                disconnectCommand = command
                disconnect(command)
            }
            is ReadCharacteristicCommand -> {
                readCharacteristicCommand = command
                readCharacteristic(command)
            }
            is WriteCharacteristicCommand -> {
                writeCharacteristicCommand = command
                writeCharacteristic(command)
            }
            is ReadDescriptorCommand -> {
                readDescriptorCommand = command
                readDescriptor(command)
            }
            is WriteDescriptorCommand -> {
                writeDescriptorCommand = command
                writeDescriptor(command)
            }
            is RequestMtuCommand -> {
                requestMtuCommand = command
                requestMtu(command)
            }
            is SetCharacteristicNotificationCommand -> {
                setCharacteristicNotificationCommand = command
                setCharacteristicNotification(command)
            }
            is ReadNotifyCommand -> {
                readNotifyCommand = command
                readNotify(command)
            }
            is ReadRemoteRssiCommand -> {
                readRemoteRssiCommand = command
                readRemoteRssi(command)
            }
            is RequestConnectionPriorityCommand -> {
                requestConnectionPriorityCommand = command
                requestConnectionPriority(command)
            }
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

    override fun close() {
        startScanCommand?.clearJobs()
        stopScanCommand?.clearJobs()
        connectCommand?.clearJobs()
        disconnectCommand?.clearJobs()
        readCharacteristicCommand?.clearJobs()
        writeCharacteristicCommand?.clearJobs()
        readDescriptorCommand?.clearJobs()
        writeDescriptorCommand?.clearJobs()
        requestMtuCommand?.clearJobs()
        setCharacteristicNotificationCommand?.clearJobs()
        readNotifyCommand?.clearJobs()
        readRemoteRssiCommand?.clearJobs()
        requestConnectionPriorityCommand?.clearJobs()
        startScanCommand = null
        stopScanCommand = null
        connectCommand = null
        disconnectCommand = null
        readCharacteristicCommand = null
        writeCharacteristicCommand = null
        readDescriptorCommand = null
        writeDescriptorCommand = null
        requestMtuCommand = null
        setCharacteristicNotificationCommand = null
        readNotifyCommand = null
        readRemoteRssiCommand = null
        requestConnectionPriorityCommand = null
    }

}
