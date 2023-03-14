package com.like.ble.central.result

import android.bluetooth.BluetoothDevice

data class ScanResult(
    val device: BluetoothDevice,
    val rssi: Int,
    val data: ByteArray?
)