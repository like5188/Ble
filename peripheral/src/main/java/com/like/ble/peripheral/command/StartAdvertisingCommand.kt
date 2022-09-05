package com.like.ble.peripheral.command

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import com.like.ble.command.Command

/**
 * 开始广播命令
 *
 * @param settings          广播的设置
 * @param advertiseData     广播的数据
 * @param scanResponse      与广播数据相关联的扫描响应数据
 * @param deviceName        设备名称。默认为手机名称。
 */
class StartAdvertisingCommand(
    val settings: AdvertiseSettings,
    val advertiseData: AdvertiseData,
    val scanResponse: AdvertiseData,
    val deviceName: String = "",
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) : Command("开始广播命令", onCompleted = onCompleted, onError = onError) {

    override fun needExecuteImmediately(): Boolean {
        return false
    }

}