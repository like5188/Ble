package com.like.ble.central.scan.callback

import android.bluetooth.BluetoothDevice
import com.like.ble.callback.BleCallback
import com.like.ble.central.util.scanrecordcompat.ScanRecordBelow21

abstract class ScanCallback : BleCallback() {
    abstract fun onSuccess(device: BluetoothDevice, rssi: Int, scanRecord: ScanRecordBelow21?)
}