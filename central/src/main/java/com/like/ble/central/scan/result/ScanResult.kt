package com.like.ble.central.scan.result

import android.bluetooth.BluetoothDevice
import com.like.ble.central.util.scanrecordcompat.ScanRecordBelow21

data class ScanResult(
    val device: BluetoothDevice,
    val rssi: Int,
    val scanRecord: ScanRecordBelow21?
)