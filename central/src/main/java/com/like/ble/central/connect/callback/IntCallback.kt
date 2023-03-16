package com.like.ble.central.connect.callback

import com.like.ble.callback.BleCallback

abstract class IntCallback : BleCallback() {
    abstract fun onSuccess(data: Int)
}