package com.like.ble.central.connect.result

import android.bluetooth.BluetoothGattService

sealed class ConnectResult {
    /**
     * 准备好开始连接了
     */
    object Ready : ConnectResult()

    /**
     * 连接成功，并返回服务列表
     */
    data class Result(val services: List<BluetoothGattService>?) : ConnectResult()

    /**
     * 连接失败
     */
    class Error(val throwable: Throwable) : ConnectResult()

}
