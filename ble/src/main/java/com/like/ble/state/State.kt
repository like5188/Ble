package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.concrete.*

/**
 * 蓝牙状态基类。
 */
abstract class State {
    lateinit var mActivity: FragmentActivity

    abstract fun init(command: InitCommand)
    abstract fun startAdvertising(command: StartAdvertisingCommand)
    abstract fun stopAdvertising(command: StopAdvertisingCommand)
    abstract fun startScan(command: StartScanCommand)
    abstract fun stopScan(command: StopScanCommand)
    abstract fun connect(command: ConnectCommand)
    abstract fun disconnect(command: DisconnectCommand)
    abstract fun readCharacteristic(command: ReadCharacteristicCommand)
    abstract fun writeCharacteristic(command: WriteCharacteristicCommand)
    abstract fun setMtu(command: SetMtuCommand)
    abstract fun close(command: CloseCommand)
}