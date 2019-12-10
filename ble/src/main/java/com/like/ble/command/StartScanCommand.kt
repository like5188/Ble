package com.like.ble.command

import com.like.ble.model.BleScanResult
import com.like.ble.receiver.IState

/**
 * 蓝牙开始扫描的命令
 *
 * @param scanTimeout               扫描超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class StartScanCommand(
    val scanTimeout: Long = 2000L,
    val onSuccess: ((BleScanResult?) -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.startScan(this)
    }

}