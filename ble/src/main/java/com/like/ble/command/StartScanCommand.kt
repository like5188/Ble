package com.like.ble.command

import android.bluetooth.BluetoothDevice

/**
 * 开始扫描蓝牙命令
 *
 * @param scanTimeout               扫描超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 */
class StartScanCommand(
    val scanTimeout: Long = 2000L,
    val onSuccess: ((BluetoothDevice, Int, ByteArray?) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver?.startScan(this)
    }

}