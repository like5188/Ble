package com.like.ble.command.concrete

import com.like.ble.command.Command

/**
 * 初始化蓝牙命令
 *
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
class InitCommand(
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver?.init(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }
}