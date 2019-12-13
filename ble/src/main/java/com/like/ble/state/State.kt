package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.concrete.*

/**
 * 蓝牙状态基类。
 */
abstract class State {
    lateinit var mActivity: FragmentActivity

    open fun init(command: InitCommand) {}
    open fun startAdvertising(command: StartAdvertisingCommand) {}
    open fun stopAdvertising(command: StopAdvertisingCommand) {}
    open fun startScan(command: StartScanCommand) {}
    open fun stopScan(command: StopScanCommand) {}
    open fun connect(command: ConnectCommand) {}
    open fun disconnect(command: DisconnectCommand) {}
    open fun readCharacteristic(command: ReadCharacteristicCommand) {}
    open fun writeCharacteristic(command: WriteCharacteristicCommand) {}
    open fun setMtu(command: SetMtuCommand) {}
    open fun close(command: CloseCommand) {
        command.successAndComplete()
    }
}