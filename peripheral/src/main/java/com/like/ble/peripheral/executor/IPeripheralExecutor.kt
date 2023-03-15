package com.like.ble.peripheral.executor

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import com.like.ble.executor.IExecutor

/**
 * 外围设备蓝牙命令执行者。
 */
interface IPeripheralExecutor : IExecutor {

    /**
     * 开始广播
     *
     * @param settings          广播的设置
     * @param advertiseData     广播的数据
     * @param scanResponse      与广播数据相关联的扫描响应数据
     * @param deviceName        设备名称。默认为设备名称。
     */
    suspend fun startAdvertising(
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData? = null,
        deviceName: String = "",
    )

    /**
     * 停止广播
     */
    fun stopAdvertising()

}
