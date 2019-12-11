package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 设置MTU命令
 *
 * @param address           蓝牙设备地址
 * @param mtu               需要设置的MTU值
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class SetMtuCommand(
    val address: String,
    val mtu: Int,
    val onSuccess: ((Int) -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver?.setMtu(this)
    }

}