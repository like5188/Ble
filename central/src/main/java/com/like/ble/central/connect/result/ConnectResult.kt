package com.like.ble.central.connect.result

import android.bluetooth.BluetoothGattService
import com.like.ble.exception.BleException

sealed class ConnectResult {
    /**
     * 准备好开始连接了
     */
    object Ready : ConnectResult()

    /**
     * 已经连接
     */
    object Connected : ConnectResult()

    /**
     * 已经断开连接
     */
    object Disconnected : ConnectResult()

    /**
     * 连接结果返回
     */
    data class Result(val services: List<BluetoothGattService>?) : ConnectResult()

    /**
     * 连接过程出错了
     */
    class Error(val throwable: Throwable) : ConnectResult() {
        constructor(message: String, code: Int = -1) : this(BleException(message, code))
    }

}
