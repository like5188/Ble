package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattService
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionCancelTimeout
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
abstract class BaseConnectExecutor(activity: ComponentActivity, private val address: String?) : AbstractConnectExecutor(activity) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }
    protected val _notifyFlow: MutableSharedFlow<ByteArray?> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val notifyFlow: Flow<ByteArray?> = _notifyFlow

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    final override suspend fun connect(timeout: Long): List<BluetoothGattService>? {
        return try {
            mutexUtils.withTryLock("正在建立连接，请稍后！") {
                checkEnvironmentOrThrow()
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "连接蓝牙设备超时：$address") { continuation ->
                        onConnect(continuation, timeout)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用着可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    null
                }
                else -> throw e
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
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用着可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
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
        return onReadDescriptor(descriptorUuid, characteristicUuid, serviceUuid, timeout)
    }

    final override suspend fun setReadNotifyCallback(characteristicUuid: UUID, serviceUuid: UUID?) {
        onSetReadNotifyCallback(characteristicUuid, serviceUuid)
    }

    final override suspend fun readRemoteRssi(timeout: Long): Int {
        return onReadRemoteRssi(timeout)
    }

    final override suspend fun requestConnectionPriority(connectionPriority: Int, timeout: Long) {
        onRequestConnectionPriority(connectionPriority, timeout)
    }

    final override suspend fun requestMtu(mtu: Int, timeout: Long): Int {
        return onRequestMtu(mtu, timeout)
    }

    final override suspend fun setCharacteristicNotification(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        timeout: Long
    ) {
        onSetCharacteristicNotification(characteristicUuid, serviceUuid, type, enable, timeout)
    }

    final override suspend fun writeCharacteristic(
        data: List<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    ) {
        onWriteCharacteristic(data, characteristicUuid, serviceUuid, timeout, writeType)
    }

    final override suspend fun writeDescriptor(
        data: List<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) {
        onWriteDescriptor(data, descriptorUuid, characteristicUuid, serviceUuid, timeout)
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
        data: List<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    )

    protected abstract fun onWriteDescriptor(
        continuation: CancellableContinuation<Unit>,
        data: List<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    )

}
