package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import com.like.ble.command.*

/**
 * 蓝牙状态基类。
 */
abstract class State {
    lateinit var mActivity: FragmentActivity

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
    open fun writeNotify(command: WriteNotifyCommand) {}
    open fun close(command: CloseCommand) {}
}