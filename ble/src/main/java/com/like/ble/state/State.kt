package com.like.ble.state

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.like.ble.command.concrete.*
import com.like.ble.model.BleResult

abstract class State {
    lateinit var mActivity: FragmentActivity
    lateinit var mLiveData: MutableLiveData<BleResult>

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