package com.like.ble.peripheral.executor

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.util.Log
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseAdvertisingExecutor(activity: ComponentActivity) : AbstractAdvertisingExecutor(activity) {
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
            mutexUtils.withTryLock("正在广播中……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout) { continuation ->
                        onStartAdvertising(continuation, settings, advertiseData, scanResponse, deviceName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TAG", e.message ?: "")
            when {
                e is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 stopAdvertising() 造成的，使用着可以直接在 stopAdvertising() 方法结束后处理 UI 的显示，不需要此回调。
                }
                e is BleException && e.code == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> {
                    // 如果正在广播，则抛出 BleExceptionBusy 异常，使得调用者可以和 withTryLock 方法抛出的异常一起统一处理。
                    throw BleExceptionBusy("正在广播中……")
                }
                else -> throw e
            }
        }
    }

    final override fun stopAdvertising() {
        if (!checkEnvironment()) {
            return
        }
        suspendCancellableCoroutineWithTimeout.cancel()
        onStopAdvertising()
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
