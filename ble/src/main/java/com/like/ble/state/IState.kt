package com.like.ble.state

import com.like.ble.command.*

interface IState {
    fun init(command: InitCommand)
    fun startAdvertising(command: StartAdvertisingCommand)
    fun stopAdvertising(command: StopAdvertisingCommand)
    fun startScan(command: StartScanCommand)
    fun stopScan(command: StopScanCommand)
    fun connect(command: ConnectCommand)
    fun disconnect(command: DisconnectCommand)
    fun readCharacteristic(command: ReadCharacteristicCommand)
    fun writeCharacteristic(command: WriteCharacteristicCommand)
    fun setMtu(command: SetMtuCommand)
    fun close(command: CloseCommand)
}