package com.like.ble.util

import com.like.ble.exception.BleException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.Continuation

/**
 * @param timeout   超时时间。如果<=0，则表示不设置超时限制。和[withTimeout]方法的参数不一样。
 * @param errorMsg  超时抛的异常[TimeoutCancellationException]会被转换成[BleException]，这是指定的错误信息。
 */
@Throws(BleException::class)
suspend inline fun <T> suspendCancellableCoroutineWithTimeout(
    timeout: Long, errorMsg: String, crossinline block: (Continuation<T>) -> Unit
): T = if (timeout > 0) {
    try {
        withTimeout(timeout) {
            suspendCancellableCoroutine(block = block)
        }
    } catch (e: TimeoutCancellationException) {
        throw BleException(errorMsg)
    }
} else {
    suspendCancellableCoroutine(block = block)
}