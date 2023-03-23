package com.like.ble.central.connect.callback

import com.like.ble.callback.BleCallback

abstract class ByteArrayCallback : BleCallback() {
    abstract fun onSuccess(data: ByteArray)
}