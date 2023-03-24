package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.activity.ComponentActivity
import com.like.ble.central.scan.executor.AbstractScanExecutor
import com.like.ble.central.scan.executor.ScanExecutor
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionCancelTimeout
import com.like.ble.exception.BleExceptionDeviceDisconnected
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import com.like.ble.util.getValidString
import com.like.ble.util.isBleDeviceConnected
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 蓝牙连接及数据操作的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
abstract class BaseConnectExecutor(activity: ComponentActivity, address: String?) : AbstractConnectExecutor(activity, address) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }
    private val scanExecutor: AbstractScanExecutor by lazy {
        ScanExecutor(activity)
    }

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    final override suspend fun connect(timeout: Long): List<BluetoothGattService> =
        try {
            mutexUtils.withTryLockOrThrow("正在建立连接，请稍后！") {
                checkEnvironmentOrThrow()
                if (context.isBleDeviceConnected(address)) {
                    throw BleExceptionBusy("设备已经连接")
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<List<BluetoothGattService>>(
                        timeout, "连接蓝牙设备超时：$address"
                    ) { continuation ->
                        continuation.invokeOnCancellation {
                            disconnect()
                        }
                        // onConnect 方法不会挂起，会在连接成功后返回，所以如果已经连接了，就抛出 BleExceptionBusy 异常
                        onConnect(continuation)
                    }
                }
            }
        } catch (e: Exception) {
            // 转换一下异常，方便使用者判断。
            throw if (e is BleException) e else BleException(e.message)
        }

    final override suspend fun scanAndConnect(timeout: Long): List<BluetoothGattService> =
        try {
            mutexUtils.withTryLockOrThrow("正在建立连接，请稍后！") {
                checkEnvironmentOrThrow()
                if (context.isBleDeviceConnected(address)) {
                    throw BleExceptionBusy("设备已经连接")
                }
                val startTime = System.currentTimeMillis()
                val scanResult = try {
                    scanExecutor.startScan(address, timeout)
                } catch (e: Exception) {
                    throw BleException("连接蓝牙失败，未找到蓝牙设备：$address")
                }
                val scanCost = System.currentTimeMillis() - startTime
                val remainTimeout = timeout - scanCost// 剩余的分配给连接的超时时间
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<List<BluetoothGattService>>(
                        remainTimeout, "连接蓝牙设备超时：$address"
                    ) { continuation ->
                        continuation.invokeOnCancellation {
                            disconnect()
                        }
                        // onConnect 方法不会挂起，会在连接成功后返回，所以如果已经连接了，就抛出 BleExceptionBusy 异常
                        onConnect(continuation, scanResult.device)
                    }
                }
            }
        } catch (e: Exception) {
            // 转换一下异常，方便使用者判断。
            throw if (e is BleException) e else BleException(e.message)
        }

    final override fun disconnect() {
        try {
            // 此处如果不取消，那么还会把超时错误传递出去的。
            suspendCancellableCoroutineWithTimeout.cancel()
            if (checkEnvironment()) {
                onDisconnect()
                scanExecutor.stopScan()
            }
        } catch (e: Exception) {
            // 转换一下异常，方便使用者判断。
            throw if (e is BleException) e else BleException(e.message)
        }
    }

    final override suspend fun readCharacteristic(characteristicUuid: UUID, serviceUuid: UUID?, timeout: Long): ByteArray {
        return try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在读取特征值……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout,
                        "读取特征值超时：${characteristicUuid.getValidString()}"
                    ) { continuation ->
                        onReadCharacteristic(continuation, characteristicUuid, serviceUuid)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    byteArrayOf()
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
    ): ByteArray {
        return try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在读取描述值……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "读取描述值超时：${descriptorUuid.getValidString()}") { continuation ->
                        onReadDescriptor(continuation, descriptorUuid, characteristicUuid, serviceUuid)
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                    byteArrayOf()
                }
                else -> throw e
            }
        }
    }

    final override suspend fun readRemoteRssi(timeout: Long): Int {
        return try {
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在读取RSSI……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "读取RSSI超时：$address") { continuation ->
                        onReadRemoteRssi(continuation)
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
            mutexUtils.withTryLockOrThrow("正在设置ConnectionPriority……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "设置ConnectionPriority超时：$address") { continuation ->
                        onRequestConnectionPriority(continuation, connectionPriority)
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
            mutexUtils.withTryLockOrThrow("正在设置MTU……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "设置MTU超时：$address") { continuation ->
                        onRequestMtu(continuation, mtu)
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
            mutexUtils.withTryLockOrThrow("正在设置通知……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "设置通知超时：$address") { continuation ->
                        onSetCharacteristicNotification(continuation, characteristicUuid, serviceUuid, type, enable)
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
            mutexUtils.withTryLockOrThrow("正在写特征值……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout,
                        "写特征值超时：${characteristicUuid.getValidString()}"
                    ) { continuation ->
                        onWriteCharacteristic(continuation, data, characteristicUuid, serviceUuid, writeType)
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
            mutexUtils.withTryLockOrThrow("正在写描述值……") {
                checkEnvironmentOrThrow()
                if (!context.isBleDeviceConnected(address)) {
                    throw BleExceptionDeviceDisconnected(address)
                }
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "写描述值超时：${descriptorUuid.getValidString()}") { continuation ->
                        onWriteDescriptor(continuation, data, descriptorUuid, characteristicUuid, serviceUuid)
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

    final override fun setNotifyCallback(characteristicUuid: UUID): Flow<ByteArray> = callbackFlow {
        onSetNotifyCallback(characteristicUuid) {
            trySend(it)
        }
        awaitClose {
            onRemoveNotifyCallback(characteristicUuid)
            Log.d("TAG", "通知监听被取消")
        }
    }

    final override fun close() {
        disconnect()
    }

    protected abstract fun onConnect(
        continuation: CancellableContinuation<List<BluetoothGattService>>,
        device: BluetoothDevice? = null
    )

    protected abstract fun onDisconnect()

    protected abstract fun onReadCharacteristic(
        continuation: CancellableContinuation<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID?
    )

    protected abstract fun onReadDescriptor(
        continuation: CancellableContinuation<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?
    )

    protected abstract fun onReadRemoteRssi(continuation: CancellableContinuation<Int>)

    protected abstract fun onRequestConnectionPriority(continuation: CancellableContinuation<Unit>, connectionPriority: Int)

    protected abstract fun onRequestMtu(continuation: CancellableContinuation<Int>, mtu: Int)

    protected abstract fun onSetCharacteristicNotification(
        continuation: CancellableContinuation<Unit>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean
    )

    protected abstract fun onWriteCharacteristic(
        continuation: CancellableContinuation<Unit>,
        data: ByteArray,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        writeType: Int
    )

    protected abstract fun onWriteDescriptor(
        continuation: CancellableContinuation<Unit>,
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?
    )

    protected abstract fun onSetNotifyCallback(characteristicUuid: UUID, onResult: (ByteArray) -> Unit)

    protected abstract fun onRemoveNotifyCallback(characteristicUuid: UUID)

}
