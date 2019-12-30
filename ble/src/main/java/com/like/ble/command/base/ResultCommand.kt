package com.like.ble.command.base

import androidx.annotation.MainThread
import com.like.ble.utils.mainThread
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 带回调的蓝牙命令
 *
 * @param onSuccess         命令执行成功回调
 * @param onFailure         命令执行失败回调
 */
abstract class ResultCommand(
    des: String,
    timeout: Long = 0L,
    private val onSuccess: BleResult? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command(des, timeout) {
    init {
        if (timeout < 0L) {
            failureAndCompleteIfIncomplete("timeout must be greater than or equal to 0")
        }
    }

    /**
     * 命令是否成功
     */
    private val mIsSuccess: AtomicBoolean = AtomicBoolean(false)

    private var mInterceptor: Interceptor? = null

    internal fun addInterceptor(interceptor: Interceptor?) {
        mInterceptor = interceptor
    }

    internal fun isSuccess() = mIsSuccess.get()

    /**
     * 命令执行成功时调用
     */
    internal fun successAndCompleteIfIncomplete(vararg args: Any?) {
        if (isCompleted()) return
        successAndComplete(*args)
    }

    /**
     * 命令执行失败时调用
     */
    internal fun failureAndCompleteIfIncomplete(errorMsg: String) {
        if (isCompleted()) return
        failureAndComplete(errorMsg)
    }

    /**
     * 在命令完成后也可以继续触发 [onSuccess] 回调。
     * 用于长连接的命令，比如[com.like.ble.command.StartAdvertisingCommand]、[com.like.ble.command.StartScanCommand]、[com.like.ble.command.ConnectCommand]。
     */
    internal fun successAndComplete(vararg args: Any?) {
        mIsSuccess.set(true)
        mainThread {
            mInterceptor?.interceptSuccess(this, *args) ?: onSuccess?.onResult(*args)
        }
        complete()
    }

    /**
     * 在命令完成后也可以继续触发 [onFailure] 回调。
     * 用于长连接的命令，比如[com.like.ble.command.StartAdvertisingCommand]、[com.like.ble.command.StartScanCommand]、[com.like.ble.command.ConnectCommand]。
     */
    internal fun failureAndComplete(errorMsg: String) {
        mIsSuccess.set(false)
        mainThread {
            val t = Throwable(errorMsg)
            mInterceptor?.interceptFailure(this, t) ?: onFailure?.invoke(t)
        }
        complete()
    }

    interface Interceptor {
        /**
         * 如果命令传入了成功回调方法，则需要重写此方法，在其中回调成功回调方法。
         */
        @MainThread
        fun interceptSuccess(command: ResultCommand, vararg args: Any?)

        /**
         * 如果命令传入了失败回调方法，则需要重写此方法，在其中回调失败回调方法。
         */
        @MainThread
        fun interceptFailure(command: ResultCommand, throwable: Throwable)
    }

    interface BleResult {
        fun onResult(vararg args: Any?)
    }

}