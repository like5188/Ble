package com.like.ble.util

import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleException
import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MutexUtils {
    val mutex = Mutex()

    @Throws(BleException::class)
    @OptIn(ExperimentalContracts::class)
    suspend inline fun <T> withTryLockOrThrow(owner: Any? = null, action: () -> T): T {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }

        if (!mutex.tryLock(owner)) throw BleExceptionBusy()
        try {
            return action()
        } finally {
            mutex.unlock(owner)
        }

    }

}
