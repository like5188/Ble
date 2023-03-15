package com.like.ble.central.executor

import com.like.ble.executor.IExecutor
import com.like.ble.result.BleResult
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 中心设备蓝牙命令执行者。
 */
interface ICentralExecutor : IExecutor {
    val scanFlow: Flow<BleResult>

    suspend fun startScan(
        filterDeviceName: String = "",
        fuzzyMatchingDeviceName: Boolean = true,
        filterDeviceAddress: String = "",
        filterServiceUuid: UUID? = null,
        duration: Long = 10000,
    )

    suspend fun stopScan()

}
