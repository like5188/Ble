package com.like.ble.callback

import com.like.ble.exception.BleException

abstract class BleCallback<T> {
    abstract fun onSuccess(data: T)

    fun onError(msg: String, code: Int = -1) {
        onError(BleException(msg, code))
    }

    open fun onError(exception: BleException) {}
}