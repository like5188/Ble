package com.like.ble.central.executor

import com.like.ble.executor.IExecutor
import com.like.ble.result.BleResult
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 中心设备蓝牙命令执行者。
 */
interface ICentralExecutor : IExecutor {

    fun startScan(
        filterDeviceName: String = "",
        fuzzyMatchingDeviceName: Boolean = true,
        filterDeviceAddress: String = "",
        filterServiceUuid: UUID? = null,
    ): Flow<BleResult>

    suspend fun stopScan()

}
