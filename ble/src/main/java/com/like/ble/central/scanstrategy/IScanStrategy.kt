package com.like.ble.central.scanstrategy

import android.bluetooth.BluetoothAdapter

interface IScanStrategy {
    fun startScan(bluetoothAdapter: BluetoothAdapter?)
    fun stopScan(bluetoothAdapter: BluetoothAdapter?)
}