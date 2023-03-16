package com.like.ble.central.callback

import android.bluetooth.BluetoothGattService

abstract class ConnectCallback : BleCallback() {
    abstract fun onSuccess(services: List<BluetoothGattService>?)
}