package com.like.ble.central.scan.result

import com.like.ble.exception.BleException

sealed class BleResult {
    object Start : BleResult()
    object Completed : BleResult()
    data class Result<T>(val data: T) : BleResult()
    class Error(val throwable: Throwable) : BleResult() {
        constructor(message: String, code: Int = -1) : this(BleException(message, code))
    }
}