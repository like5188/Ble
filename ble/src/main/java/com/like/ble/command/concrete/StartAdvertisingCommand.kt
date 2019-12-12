package com.like.ble.command.concrete

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import com.like.ble.command.Command

/**
 * 开始广播命令
 *
 * @param settings          广播的设置
 * @param advertiseData     广播的数据
 * @param scanResponse      与广播数据相关联的扫描响应数据
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class StartAdvertisingCommand(
    val settings: AdvertiseSettings,
    val advertiseData: AdvertiseData,
    val scanResponse: AdvertiseData,
    val onSuccess: (() -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver.startAdvertising(this)
    }

}