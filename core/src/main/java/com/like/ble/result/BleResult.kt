package com.like.ble.result

import com.like.ble.exception.BleException

sealed class BleResult {
    object Completed : BleResult()
    data class Result<T>(val data: T) : BleResult()
    class Error(val exception: BleException) : BleResult() {
        constructor(msg: String, code: Int = -1) : this(BleException(msg, code))
    }
}