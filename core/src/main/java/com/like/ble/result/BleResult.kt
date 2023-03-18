package com.like.ble.result

import com.like.ble.exception.BleException

sealed class BleResult {
    data class Result<T>(val data: T) : BleResult()
    class Error(val throwable: Throwable) : BleResult() {
        constructor(message: String, code: Int = -1) : this(BleException(message, code))
    }
}