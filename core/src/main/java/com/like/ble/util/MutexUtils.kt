package com.like.ble.util

import com.like.ble.exception.BleExceptionBusy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MutexUtils {
    val mutex = Mutex()

    /**
     * 当前占用锁的提示信息
     */
    var cur: String? = null

    /**
     * 当锁被占用了，就抛出[BleExceptionBusy]异常，没被占用，就执行[action]代码块
     *
     * @param busyMessage   如果当前已经占用，那么其它操作来获取锁，就会提示这个信息
     * @param action        需要执行的代码块
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <T> withTryLockOrThrow(busyMessage: String?, action: () -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }

        if (!mutex.tryLock()) throw BleExceptionBusy(cur)
        try {
            cur = busyMessage
            return action()
        } finally {
            mutex.unlock()
        }

    }

    /**
     * 等待释放锁
     */
    suspend fun waitUnlock() = withContext(Dispatchers.IO) {
        while (isActive && mutex.isLocked) {
            delay(10)
        }
    }

    fun isLocked() = mutex.isLocked

}
