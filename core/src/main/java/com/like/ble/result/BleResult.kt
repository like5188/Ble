package com.like.ble.result

import com.like.ble.exception.BleException

sealed class BleResult {
    object Completed : BleResult()
    data class Result<T>(val data: T) : BleResult()
    class Error(val msg: String, val code: Int = -1) : BleResult() {
        constructor(exception: BleException) : this(exception.msg, exception.code)
    }
}