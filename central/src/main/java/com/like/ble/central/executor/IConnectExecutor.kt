package com.like.ble.central.executor

import android.bluetooth.BluetoothGattService
import com.like.ble.executor.IExecutor

/**
 * 中心设备蓝牙命令执行者。
 */
interface IConnectExecutor : IExecutor {

    suspend fun connect(
        address: String,
        timeout: Long = 10000L,
    ): List<BluetoothGattService>?

    suspend fun disconnect()

}
