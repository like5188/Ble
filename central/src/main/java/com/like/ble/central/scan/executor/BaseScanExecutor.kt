package com.like.ble.central.scan.executor

import android.content.Context
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.toBleException
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 蓝牙扫描的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
internal abstract class BaseScanExecutor(context: Context) : AbstractScanExecutor(context) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }

    // 这里不使用 callbackFlow 是因为不需要释放资源。因为资源都在 continuation.invokeOnCancellation 中释放了。
    final override fun startScan(filterServiceUuid: UUID?, timeout: Long): Flow<ScanResult> = channelFlow {
        try {
            mutexUtils.withTryLockOrThrow("正在扫描中……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(timeout, "扫描完成") { continuation ->
                        continuation.invokeOnCancellation {
                            stopScan()
                            close()
                        }
                        onStartScan(continuation, filterServiceUuid) {
                            trySend(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 当在异步回调时，由于线程原因，会使得使用了某些中间运算符(例如firstOrNull、take)的 flow 会抛出 AbortFlowException 异常，使用者不需要处理，因为这是正常的结束并取消协程。比如 AbstractScanExecutor.startScan 方法的实现方式。
            // 当不使用异步线程时，这个异常被 AbortFlowException.checkOwnership() 方法处理了的，不会抛出。比如 AbstractConnectExecutor.setNotifyCallback 方法的实现方式。
            if (e !is CancellationException) {
                throw e.toBleException()
            }
        }
    }

    final override fun stopScan() {
        try {
            // 此处如果不取消，那么还会把超时错误传递出去的。
            suspendCancellableCoroutineWithTimeout.cancel()
            if (checkEnvironment()) {
                onStopScan()
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override fun close() {
        stopScan()
    }

    protected abstract fun onStartScan(
        continuation: CancellableContinuation<Unit>,
        filterServiceUuid: UUID?,
        onResult: (ScanResult) -> Unit
    )

    protected abstract fun onStopScan()

}
