package com.like.ble.central.scan.callback

import android.bluetooth.BluetoothDevice
import com.like.ble.callback.BleCallback

abstract class ScanCallback : BleCallback() {
    abstract fun onSuccess(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?)
}