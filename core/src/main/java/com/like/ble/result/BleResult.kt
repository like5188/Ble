package com.like.ble.result

sealed class BleResult {
    data class Success<T>(val data: T) : BleResult()
    data class Error(val msg: String, val code: Int = -1) : BleResult()
}