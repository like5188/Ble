package com.like.ble.central.scan.executor

import androidx.activity.ComponentActivity
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.exception.BleExceptionTimeout
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
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

    final override suspend fun startScan(filterServiceUuid: UUID?, duration: Long) {
        try {
            // withTryLock 方法会一直持续到调用 stopScan 或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以这里无需像外围设备处理 AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED 异常那样处理 ScanCallback.SCAN_FAILED_ALREADY_STARTED 异常。
            mutexUtils.withTryLock("正在开启扫描，请稍后！") {
                checkEnvironmentOrThrow()
                _scanFlow.tryEmit(ScanResult.Ready)
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(duration) { continuation ->
                        // onStartScan 如果不报错，会一直挂起，直到调用 stopScan 或者 suspendCancellableCoroutineWithTimeout 超时
                        onStartScan(continuation, filterServiceUuid, duration)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 stopScan() 造成的，使用着可以直接在 stopScan() 方法结束后处理 UI 的显示，不需要此回调。
                }
                is BleExceptionTimeout -> {
                    stopScan()
                    _scanFlow.tryEmit(ScanResult.Completed)
                }
                else -> {
                    _scanFlow.tryEmit(ScanResult.Error(e))
                }
            }
        }
    }

    final override fun stopScan() {
        if (!checkEnvironment()) {
            return
        }
        // 此处如果不取消，那么还会把超时错误传递出去的。
        suspendCancellableCoroutineWithTimeout.cancel()
        onStopScan()
    }

    final override fun close() {
        stopScan()
    }

    protected abstract fun onStartScan(continuation: CancellableContinuation<Unit>, filterServiceUuid: UUID?, duration: Long)

    protected abstract fun onStopScan()

}
