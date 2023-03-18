package com.like.ble.central.scan.executor

import androidx.activity.ComponentActivity
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionTimeout
import com.like.ble.result.BleResult
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 蓝牙扫描相关的命令执行者
 * 可以进行扫描、停止扫描操作
 */
abstract class BaseScanExecutor(activity: ComponentActivity) : AbstractScanExecutor(activity) {
    private val mutex = Mutex()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }
    protected val _scanFlow: MutableSharedFlow<BleResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val scanFlow: Flow<BleResult> = _scanFlow

    final override suspend fun startScan(filterServiceUuid: UUID?, duration: Long) {
        if (!mutex.tryLock()) {
            _scanFlow.tryEmit(BleResult.Error(BleExceptionBusy("正在扫描中……，请耐心等待！")))
            return
        }
        try {
            withContext(Dispatchers.IO) {
                checkEnvironmentOrThrow()
                suspendCancellableCoroutineWithTimeout.execute<Unit>(duration) { continuation ->
                    onStartScan(continuation, filterServiceUuid, duration)
                }
            }
        } catch (e: Exception) {
            if (e is BleExceptionTimeout) {
                stopScan()
            }
            _scanFlow.tryEmit(BleResult.Error(e))
        } finally {
            mutex.unlock()
        }
    }

    final override fun stopScan() {
        if (!checkEnvironment()) {
            return
        }
        suspendCancellableCoroutineWithTimeout.cancel("扫描已停止")
        onStopScan()
    }

    final override fun close() {
        stopScan()
    }

    protected abstract fun onStartScan(continuation: CancellableContinuation<Unit>, filterServiceUuid: UUID?, duration: Long)

    protected abstract fun onStopScan()

}
