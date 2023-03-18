package com.like.ble.util

import com.like.ble.exception.BleException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

class SuspendCancellableCoroutineWithTimeout {
    var cancellableContinuation: CancellableContinuation<*>? = null

    /**
     * 用于把回调转换成带超时的挂起函数
     *
     * @param timeout           超时时间。如果<=0，则表示不设置超时限制。和[withTimeout]方法的参数不一样。
     * @param timeoutErrorMsg   超时错误提示信息。
     * @throws [TimeoutCancellationException]会被转换成[BleException]
     */
    @Throws(BleException::class)
    suspend inline fun <T> execute(
        timeout: Long, timeoutErrorMsg: String, crossinline block: (CancellableContinuation<T>) -> Unit
    ): T = if (timeout > 0) {
        try {
            withTimeout(timeout) {
                // 这里不能使用 suspendCoroutine，会因为它的阻塞并且不能取消，导致 withTimeout 不能按时触发，只能在它阻塞完成后才能触发。
                suspendCancellableCoroutine {
                    cancellableContinuation = it
                    block(it)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw BleException(e)
        }
    } else {
        suspendCancellableCoroutine {
            cancellableContinuation = it
            block(it)
        }
    }

    /**
     * 取消[execute]执行的代码
     *
     * @param msg   取消提示信息
     * @throws BleException
     */
    @Throws(BleException::class)
    fun cancel(msg: String = "操作被终止") {
        cancellableContinuation?.apply {
            if (!isCancelled) {
                cancel(BleException(msg))
            }
        }
    }

}
