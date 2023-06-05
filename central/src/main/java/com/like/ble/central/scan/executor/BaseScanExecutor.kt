package com.like.ble.central.scan.executor

import android.content.Context
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.toBleException
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙扫描的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
internal abstract class BaseScanExecutor(context: Context) : AbstractScanExecutor(context) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }

    // 这里不使用 callbackFlow 是因为 awaitClose 有可能由于前面的代码抛异常，从而不能执行。所以这里需要把资源都放在 continuation.invokeOnCancellation 中释放了。
    final override fun startScan(timeout: Long): Flow<ScanResult> = channelFlow {
        try {
            checkEnvironmentOrThrow()
            mutexUtils.withTryLockOrThrow("正在扫描中……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(timeout, "扫描完成") { continuation ->
                        continuation.invokeOnCancellation {
                            stopScan()
                        }
                        onStartScan(onSuccess = {
                            trySend(it)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 当在异步回调时，由于线程原因，会使得使用了某些中间运算符(例如firstOrNull、take)的 flow 会抛出 AbortFlowException（CancellationException的子类） 异常，使用者不需要处理，因为这是正常的结束并取消协程。比如 AbstractScanExecutor.startScan 方法的实现方式。
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
        super.close()
        stopScan()
    }

    override fun onBleOff() {
        super.onBleOff()
        // 这里如果不停止扫描，那么会造成上次扫描不会终止，会直到超时完成为止，因为关闭蓝牙并不会造成 scanExecutor.startScan() 方法报错。
        stopScan()
    }

    protected abstract fun onStartScan(
        onSuccess: ((ScanResult) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onStopScan()

}
