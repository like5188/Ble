package com.like.ble.scanstrategy

import android.bluetooth.BluetoothAdapter

interface IScanStrategy {
    fun startScan(bluetoothAdapter: BluetoothAdapter?)
    fun stopScan(bluetoothAdapter: BluetoothAdapter?)
}