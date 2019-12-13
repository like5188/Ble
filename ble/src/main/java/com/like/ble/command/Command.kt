package com.like.ble.command

import com.like.ble.state.State
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙相关命令的接口
 *
 * @param des   命令功能描述
 */
abstract class Command(val des: String) {
    /**
     * 命令实际执行者
     */
    internal var mReceiver: State? = null
    /**
     * 命令是否已经完成
     */
    internal val mIsCompleted: AtomicBoolean = AtomicBoolean(false)

    /**
     * 命令执行成功时调用
     */
    internal fun success(vararg args: Any?) {
        doOnSuccess(*args)
        mIsCompleted.set(true)
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failure(throwable: Throwable) {
        doOnFailure(throwable)
        mIsCompleted.set(true)
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failure(errorMsg: String) {
        doOnFailure(Throwable(errorMsg))
        mIsCompleted.set(true)
    }

    /**
     * 执行命令
     */
    internal abstract fun execute()

    /**
     * 如果命令传入了成功回调方法，则需要重写此方法，在其中回调成功回调方法。
     */
    protected open fun doOnSuccess(vararg args: Any?) {
    }

    /**
     * 如果命令传入了失败回调方法，则需要重写此方法，在其中回调失败回调方法。
     */
    protected open fun doOnFailure(throwable: Throwable) {
    }

    override fun toString(): String {
        return "Command(des='$des')"
    }

}