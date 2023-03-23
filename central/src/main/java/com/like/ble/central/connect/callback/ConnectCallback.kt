package com.like.ble.central.connect.callback

import android.bluetooth.BluetoothGattService
import com.like.ble.callback.BleCallback

abstract class ConnectCallback : BleCallback() {
    abstract fun onSuccess(services: List<BluetoothGattService>)
}