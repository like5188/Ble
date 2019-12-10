package com.like.ble.state

import com.like.ble.command.*

abstract class StateAdapter : IState {
    override fun init(command: InitCommand) {
    }

    override fun startAdvertising(command: StartAdvertisingCommand) {
    }

    override fun stopAdvertising(command: StopAdvertisingCommand) {
    }

    override fun startScan(command: StartScanCommand) {
    }

    override fun stopScan(command: StopScanCommand) {
    }

    override fun connect(command: ConnectCommand) {
    }

    override fun disconnect(command: DisconnectCommand) {
    }

    override fun read(command: ReadCommand) {
    }

    override fun write(command: WriteCommand) {
    }

    override fun setMtu(command: SetMtuCommand) {
    }

    override fun close(command: CloseCommand) {
    }

}