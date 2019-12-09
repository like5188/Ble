package com.like.ble.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope

abstract class BleCommand(val address: String) {
    var mLiveData: MutableLiveData<BleResult>? = null

    internal open fun read(coroutineScope: CoroutineScope, bluetoothGatt: BluetoothGatt?) {}

    internal open fun write(coroutineScope: CoroutineScope, bluetoothGatt: BluetoothGatt?) {}

    internal open fun connect(coroutineScope: CoroutineScope, gattCallback: BluetoothGattCallback, bluetoothAdapter: BluetoothAdapter?, disconnect: () -> Unit) {}

    internal open fun disconnect(coroutineScope: CoroutineScope, bluetoothGatt: BluetoothGatt?) {}

    internal open fun setMtu(coroutineScope: CoroutineScope, bluetoothGatt: BluetoothGatt?) {}
}


