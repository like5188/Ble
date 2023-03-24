package com.like.ble.central.scan.executor

import androidx.activity.ComponentActivity
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.exception.BleExceptionDisabled
import com.like.ble.util.BleBroadcastReceiverManager
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import com.like.ble.util.isBluetoothEnable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import java.util.*

/**
 * 蓝牙扫描的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
abstract class BaseScanExecutor(activity: ComponentActivity) : AbstractScanExecutor(activity) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }
    private val bleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(context, onBleOff = {
            stopScan()
            _scanFlow.tryEmit(ScanResult.Error(BleExceptionDisabled))
        })
    }
    private var delayJob: Job? = null
    private var isScanning = false
    private val _scanFlow: MutableSharedFlow<ScanResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val scanFlow: Flow<ScanResult> = _scanFlow.filter {
        if (context.isBluetoothEnable()) {
            // 如果蓝牙已打开，则可以传递任何数据
            true
        } else {
            // 如果蓝牙未打开，那么就只能传递 BleExceptionDisabled 异常。避免传递其它数据对使用者造成困扰。
            it is ScanResult.Error && it.throwable is BleExceptionDisabled
        }
    }

    init {
        bleBroadcastReceiverManager.register()
    }

    final override suspend fun startScan(filterServiceUuid: UUID?, timeout: Long, duration: Long) = coroutineScope<Unit> {
        try {
            mutexUtils.withTryLockOrThrow("正在开启扫描，请稍后！") {
                checkEnvironmentOrThrow()
                if (isScanning) {
                    throw BleExceptionBusy("正在扫描中……")
                }
                isScanning = true
                _scanFlow.tryEmit(ScanResult.Ready)
                delayJob = launch(Dispatchers.IO) {
                    delay(duration)
                    stopScan()
                    _scanFlow.tryEmit(ScanResult.Completed)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "开启扫描超时") { continuation ->
                        onStartScan(continuation, filterServiceUuid) {
                            _scanFlow.tryEmit(it)
                        }
                    }
                }
                _scanFlow.tryEmit(ScanResult.Success)
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 stopScan() 造成的，使用者可以直接在 stopScan() 方法结束后处理 UI 的显示，不需要此回调。
                    isScanning = false
                }
                is BleExceptionBusy -> {
                    isScanning = true
                    _scanFlow.tryEmit(ScanResult.Error(e))
                }
                else -> {
                    stopScan()
                    _scanFlow.tryEmit(ScanResult.Error(e))
                }
            }
        }
    }

    final override suspend fun startScan(address: String?, timeout: Long): ScanResult.Result? =
        try {
            mutexUtils.withTryLockOrThrow("正在开启扫描，请稍后！") {
                checkEnvironmentOrThrow()
                isScanning = true
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<ScanResult.Result?>(timeout, "开启扫描超时") { continuation ->
                        onStartScan(continuation, address)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 stopScan() 造成的，使用者可以直接在 stopScan() 方法结束后处理 UI 的显示，不需要此回调。
                }
                else -> {
                    suspendCancellableCoroutineWithTimeout.cancel()
                    if (checkEnvironment()) {
                        onStopScan()
                    }
                }
            }
            null
        } finally {
            isScanning = false
        }

    final override fun stopScan() {
        if (!isScanning) {
            return
        }
        cancelDelayJob()
        // 此处如果不取消，那么还会把超时错误传递出去的。
        suspendCancellableCoroutineWithTimeout.cancel()
        if (checkEnvironment()) {
            onStopScan()
        }
        isScanning = false
    }

    final override fun close() {
        stopScan()
        bleBroadcastReceiverManager.unregister()
    }

    private fun cancelDelayJob() {
        delayJob?.apply {
            if (!isCancelled) {
                cancel()
            }
            delayJob = null
        }
    }

    protected abstract fun onStartScan(
        continuation: CancellableContinuation<Unit>,
        filterServiceUuid: UUID?,
        onResult: (ScanResult.Result) -> Unit
    )

    protected abstract fun onStartScan(
        continuation: CancellableContinuation<ScanResult.Result?>,
        address: String?,
    )

    protected abstract fun onStopScan()

}
