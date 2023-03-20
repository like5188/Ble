package com.like.ble.peripheral.result

import com.like.ble.exception.BleException

sealed class AdvertisingResult {
    /**
     * 准备开启广播
     */
    object Ready : AdvertisingResult()

    /**
     * 开启广播成功
     */
    object Success : AdvertisingResult()

    /**
     * 广播出错了
     */
    class Error(val throwable: Throwable) : AdvertisingResult() {
        constructor(message: String, code: Int = -1) : this(BleException(message, code))
    }

}
