package com.like.ble.central.callback

abstract class ByteArrayCallback : BleCallback() {
    abstract fun onSuccess(data: ByteArray?)
}