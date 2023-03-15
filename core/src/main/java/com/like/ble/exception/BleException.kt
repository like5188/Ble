package com.like.ble.exception

data class BleException(val msg: String, val code: Int = -1) : Exception(msg)