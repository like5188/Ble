package com.like.ble.peripheral.executor

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 外围设备广播的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
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
            mutexUtils.withTryLock("正在开启广播，请稍后！") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "开启广播超时") { continuation ->
                        onStartAdvertising(continuation, settings, advertiseData, scanResponse, deviceName)
                    }
                }
            }
        } catch (e: Exception) {
            when {
                e is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 stopAdvertising() 造成的，使用者可以直接在 stopAdvertising() 方法结束后处理 UI 的显示，不需要此回调。
                }
                e is BleException && e.code == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> {
                    // onStartAdvertising 方法不会挂起，会在广播成功后返回，所以如果设备正在广播，则把 AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED 异常转换成 BleExceptionBusy 异常抛出。
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
        // 此处如果不取消，那么还会把超时错误传递出去的。
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
