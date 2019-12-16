package com.like.ble.command

import android.bluetooth.BluetoothDevice

/**
 * 开始扫描蓝牙设备命令
 *
 * @param timeout                   命令执行超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class StartScanCommand(
    val timeout: Long = 2000L,
    private val onSuccess: ((BluetoothDevice, Int, ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("开始扫描蓝牙设备命令") {

    override fun execute() {
        mReceiver?.startScan(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.size >= 3) {
            val arg0 = args[0]
            val arg1 = args[1]
            val arg2 = args[2]
            if (arg0 is BluetoothDevice && arg1 is Int && arg2 is ByteArray?) {
                onSuccess?.invoke(arg0, arg1, arg2)
            }
        }
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }
}