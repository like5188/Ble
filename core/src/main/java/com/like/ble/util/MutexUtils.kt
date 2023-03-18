package com.like.ble.util

import com.like.ble.exception.BleExceptionBusy
import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MutexUtils {
    val mutex = Mutex()

    /**
     * 当锁被占用了，就抛出[BleExceptionBusy]异常，没被占用，就执行[action]代码块
     *
     * @param busyMessage   繁忙异常的提示信息
     * @param action        需要执行的代码块
     */
    @Throws
    @OptIn(ExperimentalContracts::class)
    inline fun <T> withTryLock(busyMessage: String?, action: () -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }

        if (!mutex.tryLock()) throw BleExceptionBusy(busyMessage)
        try {
            return action()
        } finally {
            mutex.unlock()
        }

    }

}
