package com.like.ble.sample

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    var cancellableContinuation: CancellableContinuation<*>? = null

    @Test
    fun addition_isCorrect() = runBlocking {
        channelFlow<Unit> {
            try {
                suspendCancellableCoroutine<Unit> {
                    cancellableContinuation = it
                    it.invokeOnCancellation {
                        println("invokeOnCancellation")
                        close()
                    }
                    onStartScan(it)
                    println("onStartScan")
                }
                delay(5000)
            } catch (e: Exception) {
                cancellableContinuation?.cancel()
            }
        }.catch {
            println(it)
        }.collect()
        println("finish")
    }

    private fun onStartScan(continuation: CancellableContinuation<Unit>) {
        continuation.cancel()
        println("0")
//        continuation.resume(Unit)
        println("1")
//        continuation.resumeWithException(IllegalArgumentException("err"))
        println("2")
    }
}
