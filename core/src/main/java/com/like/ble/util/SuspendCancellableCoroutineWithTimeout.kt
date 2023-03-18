package com.like.ble.util

import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.exception.BleExceptionTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

class SuspendCancellableCoroutineWithTimeout {
    var cancellableContinuation: CancellableContinuation<*>? = null

    /**
     * 用于把回调转换成带超时的挂起函数。超时会抛出[BleExceptionTimeout]异常
     *
     * @param timeout           超时时间。如果<=0，则表示不设置超时限制。和[withTimeout]方法的参数不一样。
     * @param timeoutErrorMsg   超时错误提示信息。
     */
    @Throws
    suspend inline fun <T> execute(
        timeout: Long, timeoutErrorMsg: String = "", crossinline block: (CancellableContinuation<T>) -> Unit
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
            // 超时触发 TimeoutCancellationException 异常，用指定的错误信息进行转换。其它异常不管，任其抛出
            throw BleExceptionTimeout(timeoutErrorMsg)
        }
    } else {
        suspendCancellableCoroutine {
            cancellableContinuation = it
            block(it)
        }
    }

    /**
     * 取消[execute]执行的代码
     */
    fun cancel() {
        cancellableContinuation?.apply {
            if (!isCancelled) {
                // 在 execute 方法的 suspendCancellableCoroutine 中抛出一个 BleExceptionCancelTimeout 取消超时异常
                cancel(BleExceptionCancelTimeout)
            }
        }
    }

}
