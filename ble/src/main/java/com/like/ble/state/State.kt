package com.like.ble.state

import com.like.ble.command.*

/**
 * 蓝牙状态基类。
 */
abstract class State {
    open fun startAdvertising(command: StartAdvertisingCommand) {}
    open fun stopAdvertising(command: StopAdvertisingCommand) {}
    open fun startScan(command: StartScanCommand) {}
    open fun stopScan(command: StopScanCommand) {}
    open fun connect(command: ConnectCommand) {}
    open fun disconnect(command: DisconnectCommand) {}
    open fun readCharacteristic(command: ReadCharacteristicCommand) {}
    open fun writeCharacteristic(command: WriteCharacteristicCommand) {}
    open fun setMtu(command: SetMtuCommand) {}
    open fun enableCharacteristicNotify(command: EnableCharacteristicNotifyCommand) {}
    open fun disableCharacteristicNotify(command: DisableCharacteristicNotifyCommand) {}
    open fun enableCharacteristicIndicate(command: EnableCharacteristicIndicateCommand) {}
    open fun disableCharacteristicIndicate(command: DisableCharacteristicIndicateCommand) {}
    open fun writeAndWaitForData(command: WriteAndWaitForDataCommand) {}
    open fun readRemoteRssi(command: ReadRemoteRssiCommand) {}
    open fun requestConnectionPriority(command: RequestConnectionPriorityCommand) {}
    open fun close(command: CloseCommand) {}
}