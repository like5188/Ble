package com.like.ble.central.connect.result

import android.bluetooth.BluetoothGattService

interface NotifyResult {
    /**
     * 准备开启连接
     */
    object Ready : NotifyResult()

    /**
     * 连接成功，并返回服务列表
     */
    data class onReceived(val services: List<BluetoothGattService>?) : NotifyResult()

    /**
     * 连接失败
     */
    class Error(val throwable: Throwable) : NotifyResult()

}
