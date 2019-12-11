package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 断开蓝牙连接命令
 *
 * @param address           蓝牙设备地址
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class DisconnectCommand(
    val address: String,
    val onSuccess: (() -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver.disconnect(this)
    }

}