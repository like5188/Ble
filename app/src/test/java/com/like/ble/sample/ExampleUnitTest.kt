package com.like.ble.sample

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() = runBlocking {
        val timeout = 1000L
        val cost = measureTimeMillis {
            val startTime = System.currentTimeMillis()
            val addressesTemp = mutableListOf("1", "4", "2")
            val scanAddresses = flowOf("1", "1", "4", "4", "2", "5", "3", "6", "7", "8")
                .onEach {
                    delay(100)
                }
                .filter {
                    addressesTemp.contains(it) && addressesTemp.remove(it)
                }
                .take(3)
                .toList()
            val cost = System.currentTimeMillis() - startTime
            val remainTime = timeout - cost
            println("scanAddresses=$scanAddresses timeout=$timeout cost=$cost remainTime=$remainTime")
            withContext(Dispatchers.IO) {
                scanAddresses.forEach {
                    launch {
                        try {
                            if (it == "1") throw RuntimeException("error 1")
                            if (it == "2") throw RuntimeException("error 2")
                            delay(remainTime)
                            println("connect success $it")
                        } catch (e: Exception) {
                            println("connect failure ${e.message}")
                        }
                    }
                }
            }
        }
        println("finish $cost")
    }

}
