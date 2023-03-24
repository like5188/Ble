package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.activity.ComponentActivity
import com.like.ble.central.connect.result.ConnectResult
import com.like.ble.central.scan.executor.AbstractScanExecutor
import com.like.ble.central.scan.executor.ScanExecutor
import com.like.ble.central.scan.result.ScanResult
import com.like.ble.exception.*
import com.like.ble.util.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
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

    // 是否需要重新扫描，当断开蓝牙再打开，如果不重新扫描，则连接不上。
    private var needScan = false
    private val scanExecutor: AbstractScanExecutor by lazy {
        ScanExecutor(activity)
    }

    private val bleBroadcastReceiverManager by lazy {
        BleBroadcastReceiverManager(context, onBleOff = {
            disconnect()
            _connectFlow.tryEmit(ConnectResult.Error(BleExceptionDisabled))
        })
    }
    protected val _connectFlow: MutableSharedFlow<ConnectResult> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val connectFlow: Flow<ConnectResult> = _connectFlow.filter {
        if (context.isBluetoothEnable()) {
            // 如果蓝牙已打开，则可以传递任何数据
            true
        } else {
            // 如果蓝牙未打开，那么就只能传递 BleExceptionDisabled 异常。避免传递其它数据对使用者造成困扰。
            val isBleDisabled = it is ConnectResult.Error && it.throwable is BleExceptionDisabled
            if (isBleDisabled) {
                // 蓝牙关闭时，需要重新扫描才能连接上设备。
                needScan = true
            }
            // 其它类型时，needScan 保持不变
            isBleDisabled
        }
    }
    protected val _notifyFlow: MutableSharedFlow<BluetoothGattCharacteristic> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    final override val notifyFlow: Flow<BluetoothGattCharacteristic> = _notifyFlow

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
        bleBroadcastReceiverManager.register()
    }

    final override suspend fun connect(timeout: Long) {
        try {
            mutexUtils.withTryLockOrThrow("正在建立连接，请稍后！") {
                checkEnvironmentOrThrow()
                if (context.isBleDeviceConnected(address)) {
                    throw BleExceptionBusy("设备已经连接")
                }
                _connectFlow.tryEmit(ConnectResult.Ready)
                var remainTimeout = timeout// 剩余的分配给连接的超时时间
                var scanResult: ScanResult? = null// 扫描结果
                Log.d("TAG", "connect needScan=$needScan")
                if (needScan) {
                    val startTime = System.currentTimeMillis()
                    scanResult = scanExecutor.startScan(address, timeout) ?: throw BleException("连接蓝牙失败：$address 未找到")
                    // 扫描成功后，下次就不需要再扫描了
                    needScan = false
                    val scanCost = System.currentTimeMillis() - startTime
                    remainTimeout -= scanCost
                    Log.d(
                        "TAG",
                        "connect timeout=$timeout scanCost=$scanCost remainTimeout=$remainTimeout"
                    )
                }
                withContext(Dispatchers.IO) {
                    val services = suspendCancellableCoroutineWithTimeout.execute(remainTimeout, "连接蓝牙设备超时：$address") { continuation ->
                        // onConnect 方法不会挂起，会在连接成功后返回，所以如果已经连接了，就抛出 BleExceptionBusy 异常
                        onConnect(continuation, scanResult?.device)
                    }
                    _connectFlow.tryEmit(ConnectResult.Result(services))
                }
            }
        } catch (e: Exception) {
            when (e) {
                is BleExceptionCancelTimeout -> {
                    // 提前取消超时不做处理。因为这是调用 disconnect() 造成的，使用者可以直接在 disconnect() 方法结束后处理 UI 的显示，不需要此回调。
                }
                else -> {
                    _connectFlow.tryEmit(ConnectResult.Error(e))
                }
            }
        }
    }

    final override fun disconnect() {
        // 此处如果不取消，那么还会把超时错误传递出去的。
        suspendCancellableCoroutineWithTimeout.cancel()
        if (checkEnvironment()) {
            onDisconnect()
            scanExecutor.stopScan()
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

    final override fun close() {
        disconnect()
        bleBroadcastReceiverManager.unregister()
    }

    protected abstract fun onConnect(
        continuation: CancellableContinuation<List<BluetoothGattService>>,
        device: BluetoothDevice?
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

}
