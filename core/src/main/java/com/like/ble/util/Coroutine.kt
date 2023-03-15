package com.like.ble.util

import com.like.ble.exception.BleException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * 用于把回调转换成带超时的挂起函数
 *
 * @param timeout   超时时间。如果<=0，则表示不设置超时限制。和[withTimeout]方法的参数不一样。
 * @param errorMsg  超时抛的异常[TimeoutCancellationException]会被转换成[BleException]，这是指定的错误信息。
 */
@Throws(BleException::class)
suspend inline fun <T> suspendCancellableCoroutineWithTimeout(
    timeout: Long, errorMsg: String, crossinline block: (CancellableContinuation<T>) -> Unit
): T = if (timeout > 0) {
    try {
        withTimeout(timeout) {
            // 这里不能使用 suspendCoroutine，会因为它的阻塞并且不能取消，导致 withTimeout 不能按时触发，只能在它阻塞完成后才能触发。
            suspendCancellableCoroutine(block = block)
        }
    } catch (e: TimeoutCancellationException) {
        throw BleException(errorMsg)
    }
} else {
    suspendCancellableCoroutine(block = block)
}