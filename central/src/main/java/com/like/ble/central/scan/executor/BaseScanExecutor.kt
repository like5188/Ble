package com.like.ble.central.scan.executor

import android.content.Context
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.toBleException
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
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

    // 不知道为什么，如果这里使用callbackFlow，那么使用awaitClose时总是报错
    final override fun startScan(filterServiceUuid: UUID?, timeout: Long): Flow<ScanResult> = channelFlow {
        try {
            mutexUtils.withTryLockOrThrow("正在扫描中……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(timeout, "扫描完成") { continuation ->
                        continuation.invokeOnCancellation {
                            stopScan()
                        }
                        onStartScan(continuation, filterServiceUuid) {
                            trySend(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override suspend fun startScan(address: String?, timeout: Long): ScanResult =
        try {
            mutexUtils.withTryLockOrThrow("正在扫描中……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<ScanResult>(timeout, "未找到设备：$address") { continuation ->
                        continuation.invokeOnCancellation {
                            stopScan()
                        }
                        onStartScan(continuation, address)
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
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

    protected abstract fun onStartScan(
        continuation: CancellableContinuation<ScanResult>,
        address: String?,
    )

    protected abstract fun onStopScan()

}
