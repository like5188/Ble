package com.like.ble.peripheral.executor

import android.Manifest
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.executor.BleExecutor

/**
 * 外围设备广播执行者。
 */
abstract class AbstractAdvertisingExecutor(activity: ComponentActivity) : BleExecutor(
    activity,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        emptyArray()
    }
) {

    /**
     * 开始广播
     *
     * @param settings          广播的设置
     * @param advertiseData     广播的数据
     * @param scanResponse      与广播数据相关联的扫描响应数据
     * @param deviceName        设备名称。默认为设备名称。
     */
    abstract suspend fun startAdvertising(
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData? = null,
        deviceName: String = "",
    )

    /**
     * 停止广播
     */
    abstract fun stopAdvertising()

}
