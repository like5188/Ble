package com.like.ble.command

import com.like.ble.state.IState

/**
 * 连接蓝牙命令
 *
 * @param address           蓝牙设备地址
 * @param connectTimeout    连接超时时间（毫秒）
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class ConnectCommand(
    val address: String,
    val connectTimeout: Long = 20000L,
    val onSuccess: (() -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : ICommand {
    var mReceiver: IState? = null

    override fun execute() {
        mReceiver?.connect(this)
    }

}