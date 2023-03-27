package com.like.ble.peripheral.executor

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.toBleException
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 外围设备广播的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
abstract class BaseAdvertisingExecutor(context: Context) : AbstractAdvertisingExecutor(context) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }

    final override suspend fun startAdvertising(
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData?,
        deviceName: String,
        timeout: Long
    ) {
        try {
            mutexUtils.withTryLockOrThrow("正在开启广播，请稍后！") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(timeout, "开启广播超时") { continuation ->
                        continuation.invokeOnCancellation {
                            stopAdvertising()
                        }
                        onStartAdvertising(continuation, settings, advertiseData, scanResponse, deviceName)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is BleException && e.code == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                throw BleExceptionBusy("正在广播中……")
            }
            throw e.toBleException()
        }
    }

    final override fun stopAdvertising() {
        try {
            // 此处如果不取消，那么还会把超时错误传递出去的。
            suspendCancellableCoroutineWithTimeout.cancel()
            if (checkEnvironment()) {
                onStopAdvertising()
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override fun close() {
        stopAdvertising()
    }

    protected abstract fun onStartAdvertising(
        continuation: CancellableContinuation<Unit>,
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData?,
        deviceName: String
    )

    protected abstract fun onStopAdvertising()

}
