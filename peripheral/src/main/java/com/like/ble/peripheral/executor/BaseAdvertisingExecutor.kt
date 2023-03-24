package com.like.ble.peripheral.executor

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.exception.BleExceptionDisabled
import com.like.ble.peripheral.result.AdvertisingResult
import com.like.ble.util.BleBroadcastReceiverManager
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import com.like.ble.util.isBluetoothEnable
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
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
    private val bleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(context, onBleOff = {
            stopAdvertising()
            _advertisingFlow.tryEmit(AdvertisingResult.Error(BleExceptionDisabled))
        })
    }
    private var isAdvertising = false
    private val _advertisingFlow: MutableSharedFlow<AdvertisingResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val advertisingFlow: Flow<AdvertisingResult> = _advertisingFlow.filter {
        if (context.isBluetoothEnable()) {
            // 如果蓝牙已打开，则可以传递任何数据
            true
        } else {
            // 如果蓝牙未打开，那么就只能传递 BleExceptionDisabled 异常。避免传递其它数据对使用者造成困扰。
            it is AdvertisingResult.Error && it.throwable is BleExceptionDisabled
        }
    }

    init {
        bleBroadcastReceiverManager.register()
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
                if (isAdvertising) {
                    throw BleExceptionBusy("正在广播中……")
                }
                isAdvertising = true
                _advertisingFlow.tryEmit(AdvertisingResult.Ready)
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "开启广播超时") { continuation ->
                        onStartAdvertising(continuation, settings, advertiseData, scanResponse, deviceName)
                    }
                }
                _advertisingFlow.tryEmit(AdvertisingResult.Success)
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 stopAdvertising() 造成的，使用者可以直接在 stopAdvertising() 方法结束后处理 UI 的显示，不需要此回调。
                    isAdvertising = false
                }
                is BleExceptionBusy -> {
                    isAdvertising = true
                    _advertisingFlow.tryEmit(AdvertisingResult.Error(e))
                }
                else -> {
                    stopAdvertising()
                    _advertisingFlow.tryEmit(AdvertisingResult.Error(e))
                }
            }
        }
    }

    final override fun stopAdvertising() {
        if (!isAdvertising) {
            return
        }
        // 此处如果不取消，那么还会把超时错误传递出去的。
        suspendCancellableCoroutineWithTimeout.cancel()
        if (checkEnvironment()) {
            onStopAdvertising()
        }
        isAdvertising = false
    }

    final override fun close() {
        stopAdvertising()
        bleBroadcastReceiverManager.unregister()
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
