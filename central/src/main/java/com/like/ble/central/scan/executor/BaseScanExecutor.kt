package com.like.ble.central.scan.executor

import android.widget.Toast
import androidx.activity.ComponentActivity
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
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
abstract class BaseScanExecutor(activity: ComponentActivity) : AbstractScanExecutor(activity) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }

    final override fun startScan(filterServiceUuid: UUID?, timeout: Long): Flow<ScanResult> = channelFlow {
        try {
            mutexUtils.withTryLockOrThrow("正在扫描中……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<ScanResult>(timeout, "扫描完成") { continuation ->
                        continuation.invokeOnCancellation {
                            stopScan()
                        }
                        onStartScan(filterServiceUuid) {
                            trySend(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is BleExceptionBusy) {
                // 此处不能close。
                Toast.makeText(context, "is scanning ……", Toast.LENGTH_SHORT).show()
                return@channelFlow
            }
            throw if (e is BleException) e else BleException(e.message)
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
            // 转换一下异常，方便使用者判断。
            throw if (e is BleException) e else BleException(e.message)
        }

    final override fun stopScan() {
        try {
            // 此处如果不取消，那么还会把超时错误传递出去的。
            suspendCancellableCoroutineWithTimeout.cancel()
            if (checkEnvironment()) {
                onStopScan()
            }
        } catch (e: Exception) {
            // 转换一下异常，方便使用者判断。
            throw if (e is BleException) e else BleException(e.message)
        }
    }

    final override fun close() {
        stopScan()
    }

    protected abstract fun onStartScan(
        filterServiceUuid: UUID?,
        onResult: (ScanResult) -> Unit
    )

    protected abstract fun onStartScan(
        continuation: CancellableContinuation<ScanResult>,
        address: String?,
    )

    protected abstract fun onStopScan()

}
