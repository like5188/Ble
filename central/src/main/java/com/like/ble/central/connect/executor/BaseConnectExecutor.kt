package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattService
import androidx.activity.ComponentActivity
import com.like.ble.central.connect.result.ConnectResult
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.exception.BleExceptionDiscoverServices
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import com.like.ble.util.getValidString
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 蓝牙连接及数据操作的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
abstract class BaseConnectExecutor(activity: ComponentActivity, protected val address: String?) : AbstractConnectExecutor(activity) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }
    protected val _connectFlow: MutableSharedFlow<ConnectResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val connectFlow: Flow<ConnectResult> = _connectFlow
    protected val _notifyFlow: MutableSharedFlow<ByteArray?> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val notifyFlow: Flow<ByteArray?> = _notifyFlow

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    final override suspend fun connect(timeout: Long) {
        try {
            mutexUtils.withTryLock("正在建立连接，请稍后！") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    val services = suspendCancellableCoroutineWithTimeout.execute(timeout, "连接蓝牙设备超时：$address") { continuation ->
                        // onConnect 方法不会挂起，会在连接成功后返回，所以如果已经连接了，就抛出 BleExceptionBusy 异常
                        onConnect(continuation, timeout)
                    }
                    _connectFlow.tryEmit(ConnectResult.Result(services))
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                }
                is BleExceptionDiscoverServices -> {
                    disconnect()
                    _connectFlow.tryEmit(ConnectResult.Error(e))
                }
                else -> {
                    _connectFlow.tryEmit(ConnectResult.Error(e))
                }
            }
        }
    }

    final override fun disconnect() {
        if (!checkEnvironment()) {
            return
        }
        // 此处如果不取消，那么还会把超时错误传递出去的。
        suspendCancellableCoroutineWithTimeout.cancel()
        onDisconnect()
    }

    final override suspend fun readCharacteristic(characteristicUuid: UUID, serviceUuid: UUID?, timeout: Long): ByteArray? {
        return try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在读取特征值……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "读取特征值超时：${characteristicUuid.getValidString()}"
                    ) { continuation ->
                        onReadCharacteristic(continuation, characteristicUuid, serviceUuid, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    null
                }
                else -> throw e
            }
        }
    }

    final override suspend fun readDescriptor(
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ): ByteArray? {
        return try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在读取描述值……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "读取描述值超时：${descriptorUuid.getValidString()}"
                    ) { continuation ->
                        onReadDescriptor(continuation, descriptorUuid, characteristicUuid, serviceUuid, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    null
                }
                else -> throw e
            }
        }
    }

    final override suspend fun setReadNotifyCallback(characteristicUuid: UUID, serviceUuid: UUID?) {
        checkEnvironmentOrThrow()
        onSetReadNotifyCallback(characteristicUuid, serviceUuid)
    }

    final override suspend fun readRemoteRssi(timeout: Long): Int {
        return try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在读取RSSI……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "读取RSSI超时：$address"
                    ) { continuation ->
                        onReadRemoteRssi(continuation, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    -1
                }
                else -> throw e
            }
        }
    }

    final override suspend fun requestConnectionPriority(connectionPriority: Int, timeout: Long) {
        try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在设置ConnectionPriority……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "设置ConnectionPriority超时：$address"
                    ) { continuation ->
                        onRequestConnectionPriority(continuation, connectionPriority, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                }
                else -> throw e
            }
        }
    }

    final override suspend fun requestMtu(mtu: Int, timeout: Long): Int {
        return try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在设置MTU……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "设置MTU超时：$address"
                    ) { continuation ->
                        onRequestMtu(continuation, mtu, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    -1
                }
                else -> throw e
            }
        }
    }

    final override suspend fun setCharacteristicNotification(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        timeout: Long
    ) {
        try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在设置通知……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "设置通知超时：$address"
                    ) { continuation ->
                        onSetCharacteristicNotification(continuation, characteristicUuid, serviceUuid, type, enable, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                }
                else -> throw e
            }
        }
    }

    final override suspend fun writeCharacteristic(
        data: ByteArray,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    ) {
        try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在写特征值……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "写特征值超时：${characteristicUuid.getValidString()}"
                    ) { continuation ->
                        onWriteCharacteristic(continuation, data, characteristicUuid, serviceUuid, timeout, writeType)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                }
                else -> throw e
            }
        }
    }

    final override suspend fun writeDescriptor(
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) {
        try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLock("正在写描述值……") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout, "写描述值超时：${descriptorUuid.getValidString()}"
                    ) { continuation ->
                        onWriteDescriptor(continuation, data, descriptorUuid, characteristicUuid, serviceUuid, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                }
                else -> throw e
            }
        }
    }

    final override fun close() {
        disconnect()
    }

    protected abstract fun onConnect(
        continuation: CancellableContinuation<List<BluetoothGattService>?>,
        timeout: Long
    )

    protected abstract fun onDisconnect()

    protected abstract fun onReadCharacteristic(
        continuation: CancellableContinuation<ByteArray?>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long
    )

    protected abstract fun onReadDescriptor(
        continuation: CancellableContinuation<ByteArray?>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    )

    protected abstract fun onSetReadNotifyCallback(characteristicUuid: UUID, serviceUuid: UUID?)

    protected abstract fun onReadRemoteRssi(continuation: CancellableContinuation<Int>, timeout: Long)

    protected abstract fun onRequestConnectionPriority(continuation: CancellableContinuation<Unit>, connectionPriority: Int, timeout: Long)

    protected abstract fun onRequestMtu(continuation: CancellableContinuation<Int>, mtu: Int, timeout: Long)

    protected abstract fun onSetCharacteristicNotification(
        continuation: CancellableContinuation<Unit>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        timeout: Long
    )

    protected abstract fun onWriteCharacteristic(
        continuation: CancellableContinuation<Unit>,
        data: ByteArray,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    )

    protected abstract fun onWriteDescriptor(
        continuation: CancellableContinuation<Unit>,
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    )

}
