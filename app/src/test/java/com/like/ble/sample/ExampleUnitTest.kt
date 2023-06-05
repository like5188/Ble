package com.like.ble.sample

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() = runBlocking {
        val cost = measureTimeMillis {
            channelFlow {
                kotlinx.coroutines.withTimeout(10000) {
                    suspendCancellableCoroutine {
                        it.invokeOnCancellation {
                            println("invokeOnCancellation")
                        }
                        thread {
                            (0..4).forEach {
                                Thread.sleep(1000)
                                trySend(1)
                            }
                        }
                    }
                }
            }.take(2).collectLatest {
                println(it)
            }
        }
        println("finish $cost")
    }

    private fun onStartScan(continuation: CancellableContinuation<Unit>) {
        continuation.cancel()
        println("0")
//        continuation.resume(Unit)
//        println("1")
//        continuation.resumeWithException(IllegalArgumentException("err"))
//        println("2")
    }
}
