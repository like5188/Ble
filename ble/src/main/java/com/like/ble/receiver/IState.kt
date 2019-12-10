package com.like.ble.receiver

import com.like.ble.command.*

interface IState {
    fun init(command: InitCommand)
    fun startAdvertising(command: StartAdvertisingCommand)
    fun stopAdvertising(command: StopAdvertisingCommand)
    fun startScan(command: StartScanCommand)
    fun stopScan(command: StopScanCommand)
    fun connect(command: ConnectCommand)
    fun disconnect(command: DisconnectCommand)
    fun read(command: ReadCommand)
    fun write(command: WriteCommand)
    fun setMtu(command: SetMtuCommand)
    fun close(command: CloseCommand)
}