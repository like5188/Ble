package com.like.ble.util

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.Continuation

/**
 * @param timeout   超时时间。如果<=0，则表示不设置超时限制。和[withTimeout]方法的参数不一样。
 */
suspend inline fun <T> suspendCancellableCoroutineWithTimeout(
    timeout: Long, crossinline block: (Continuation<T>) -> Unit
): T = if (timeout > 0) {
    withTimeout(timeout) {
        suspendCancellableCoroutine(block = block)
    }
} else {
    suspendCancellableCoroutine(block = block)
}