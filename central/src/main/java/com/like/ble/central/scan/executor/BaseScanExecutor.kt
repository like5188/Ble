package com.like.ble.central.scan.executor

import android.bluetooth.le.ScanCallback
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    protected val _scanFlow: MutableSharedFlow<ScanResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val scanFlow: Flow<ScanResult> = _scanFlow
    private var delayJob: Job? = null

    final override suspend fun startScan(filterServiceUuid: UUID?, timeout: Long, duration: Long) {
        try {
            mutexUtils.withTryLock("正在开启扫描，请稍后！") {
                checkEnvironmentOrThrow()
                _scanFlow.tryEmit(ScanResult.Ready)
                delayJob = activity.lifecycleScope.launch {
                    delay(duration)
                    stopScan()
                    _scanFlow.tryEmit(ScanResult.Completed)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "开启扫描超时") { continuation ->
                        onStartScan(continuation, filterServiceUuid)
                    }
                }
            }
        } catch (e: Exception) {
            when {
                e is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 stopScan() 造成的，使用者可以直接在 stopScan() 方法结束后处理 UI 的显示，不需要此回调。
                }
                e is BleException && e.code == ScanCallback.SCAN_FAILED_ALREADY_STARTED -> {
                    // onStartAdvertising 方法不会挂起，会在广播成功后返回，所以如果设备正在广播，则把 AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED 异常转换成 BleExceptionBusy 异常抛出。
                    _scanFlow.tryEmit(ScanResult.Error(BleExceptionBusy("正在扫描中……")))
                }
                else -> {
                    cancelDelayJob()
                    _scanFlow.tryEmit(ScanResult.Error(e))
                }
            }
        }
    }

    final override fun stopScan() {
        if (!checkEnvironment()) {
            return
        }
        cancelDelayJob()
        // 此处如果不取消，那么还会把超时错误传递出去的。
        suspendCancellableCoroutineWithTimeout.cancel()
        onStopScan()
    }

    final override fun close() {
        stopScan()
    }

    private fun cancelDelayJob() {
        delayJob?.apply {
            if (!isCancelled) {
                cancel()
            }
            delayJob = null
        }
    }

    protected abstract fun onStartScan(continuation: CancellableContinuation<Unit>, filterServiceUuid: UUID?)

    protected abstract fun onStopScan()

}
