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
 */
class StartAdvertisingCommand(
    val settings: AdvertiseSettings,
    val advertiseData: AdvertiseData,
    val scanResponse: AdvertiseData
) : Command() {

    override fun execute() {
        mReceiver.startAdvertising(this)
    }

}