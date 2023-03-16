package com.like.ble.central.callback

abstract class IntCallback : BleCallback() {
    abstract fun onSuccess(data: Int)
}