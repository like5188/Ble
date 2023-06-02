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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 外围设备广播的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
internal abstract class BaseAdvertisingExecutor(context: Context) : AbstractAdvertisingExecutor(context) {
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
                        onStartAdvertising(settings, advertiseData, scanResponse, deviceName, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(Unit)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
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
        super.close()
        stopAdvertising()
    }

    override fun onBleOff() {
        super.onBleOff()
        stopAdvertising()
    }

    protected abstract fun onStartAdvertising(
        settings: AdvertiseSettings,
        advertiseData: AdvertiseData,
        scanResponse: AdvertiseData?,
        deviceName: String,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onStopAdvertising()

}
