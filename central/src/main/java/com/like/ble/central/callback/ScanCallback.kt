package com.like.ble.central.callback

import android.bluetooth.BluetoothDevice

abstract class ScanCallback : BleCallback() {
    abstract fun onSuccess(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?)
}