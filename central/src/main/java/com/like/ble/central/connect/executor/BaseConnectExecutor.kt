package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionDeviceDisconnected
import com.like.ble.exception.toBleException
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import com.like.ble.util.getValidString
import com.like.ble.util.isBleDeviceConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙连接及数据操作的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
internal abstract class BaseConnectExecutor(context: Context, address: String?) : AbstractConnectExecutor(context, address) {
    private val mutexUtils = MutexUtils()
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    final override suspend fun connect(
        timeout: Long,
        onDisconnectedListener: ((Throwable) -> Unit)?
    ): List<BluetoothGattService> = try {
        checkEnvironmentOrThrow()
        if (mContext.isBleDeviceConnected(address)) {
            throw BleExceptionBusy("设备已经连接：$address")
        }
        mutexUtils.withTryLockOrThrow("正在建立连接，请稍后！") {
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutineWithTimeout.execute(
                    timeout, "连接蓝牙设备超时：$address"
                ) { continuation ->
                    continuation.invokeOnCancellation {
                        disconnect()
                    }
                    onConnect(onDisconnectedListener, onSuccess = {
                        if (continuation.isActive)
                            continuation.resume(it)
                    }) {
                        if (continuation.isActive)
                            continuation.resumeWithException(it)
                    }
                }
            }
        }
    } catch (e: Exception) {
        throw e.toBleException()
    }

    final override fun disconnect() {
        try {
            // 此处如果不取消，那么还会把超时错误传递出去的。
            suspendCancellableCoroutineWithTimeout.cancel()
            if (checkEnvironment()) {
                onDisconnect()
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override suspend fun readCharacteristic(characteristicUuid: UUID, serviceUuid: UUID?, timeout: Long): ByteArray {
        return try {
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在读取特征值……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(
                        timeout,
                        "读取特征值超时：${characteristicUuid.getValidString()}"
                    ) { continuation ->
                        onReadCharacteristic(characteristicUuid, serviceUuid, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(it)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override suspend fun readDescriptor(
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ): ByteArray {
        return try {
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在读取描述值……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "读取描述值超时：${descriptorUuid.getValidString()}") { continuation ->
                        onReadDescriptor(descriptorUuid, characteristicUuid, serviceUuid, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(it)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override suspend fun readRemoteRssi(timeout: Long): Int {
        return try {
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在读取RSSI……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "读取RSSI超时：$address") { continuation ->
                        onReadRemoteRssi(onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(it)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override suspend fun requestConnectionPriority(connectionPriority: Int, timeout: Long) {
        try {
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在设置ConnectionPriority……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(timeout, "设置ConnectionPriority超时：$address") { continuation ->
                        onRequestConnectionPriority(connectionPriority, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(Unit)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override suspend fun requestMtu(mtu: Int, timeout: Long): Int {
        return try {
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在设置MTU……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute(timeout, "设置MTU超时：$address") { continuation ->
                        onRequestMtu(mtu, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(it)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
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
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在设置通知……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(timeout, "设置通知超时：$address") { continuation ->
                        onSetCharacteristicNotification(characteristicUuid, serviceUuid, type, enable, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(Unit)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
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
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在写特征值……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(
                        timeout,
                        "写特征值超时：${characteristicUuid.getValidString()}"
                    ) { continuation ->
                        onWriteCharacteristic(data, characteristicUuid, serviceUuid, writeType, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(Unit)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 转换一下异常，方便使用者判断。
            throw e.toBleException()
        }
    }

    final override suspend fun writeCharacteristicAndWaitNotify(
        data: ByteArray,
        writeUuid: UUID,
        notifyUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long,
        notifyType: Int,
        writeType: Int,
        isWholePackage: (ByteArray) -> Boolean
    ): ByteArray = try {
        checkEnvironmentOrThrow()
        if (!mContext.isBleDeviceConnected(address)) {
            throw BleExceptionDeviceDisconnected(address)
        }
        mutexUtils.withTryLockOrThrow("正在写特征值……") {
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutineWithTimeout.execute(
                    timeout, "写特征值超时：${writeUuid.getValidString()}"
                ) { continuation ->
                    if (notifyUuid != null) {
                        // 启用通知
                        onSetCharacteristicNotification(notifyUuid, serviceUuid, notifyType, true) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(it)
                                return@onSetCharacteristicNotification
                            }
                        }
                        // 延迟100毫秒，否则下面的写入命令会失败。
                        Thread.sleep(100)
                    }
                    // 设置监听
                    var result: ByteArray = byteArrayOf()
                    onSetNotifyCallback {
                        Log.d("BaseConnectExecutor", "获取到数据 ${it.contentToString()}")
                        result += it
                        if (isWholePackage(result)) {
                            Log.d("BaseConnectExecutor", "获取到了完整数据包 ${result.contentToString()}")
                            // 取消监听
                            onRemoveNotifyCallback()
                            Log.d("BaseConnectExecutor", "通知监听被取消")
                            if (continuation.isActive)
                                continuation.resume(result)
                        }
                    }
                    // 写入命令
                    onWriteCharacteristic(data, writeUuid, serviceUuid, writeType) {
                        if (continuation.isActive)
                            continuation.resumeWithException(it)
                    }
                }
            }
        }
    } catch (e: Exception) {
        throw e.toBleException()
    }

    final override suspend fun writeDescriptor(
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) {
        try {
            checkEnvironmentOrThrow()
            if (!mContext.isBleDeviceConnected(address)) {
                throw BleExceptionDeviceDisconnected(address)
            }
            // withTryLock 方法会一直持续到命令执行完成或者 suspendCancellableCoroutineWithTimeout 超时，这段时间是一直上锁了的，
            // 所以不会产生 BleExceptionBusy 异常。
            mutexUtils.withTryLockOrThrow("正在写描述值……") {
                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutineWithTimeout.execute<Unit>(
                        timeout,
                        "写描述值超时：${descriptorUuid.getValidString()}"
                    ) { continuation ->
                        onWriteDescriptor(data, descriptorUuid, characteristicUuid, serviceUuid, onSuccess = {
                            if (continuation.isActive)
                                continuation.resume(Unit)
                        }) {
                            if (continuation.isActive)
                                continuation.resumeWithException(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e.toBleException()
        }
    }

    final override fun setNotifyCallback(): Flow<ByteArray> = callbackFlow {
        onSetNotifyCallback {
            trySend(it)
        }
        // 注意：不能在这个作用域里面使用挂起函数，这样会导致使用者使用 cancel 方法关闭协程作用域的时候，
        // 因为还没有执行到 awaitClose 方法，所以就触发不了 awaitClose 里面的代码。所以如果要使用挂起函数，有两种方式：
        // 1、使用 launch 方法重新开启一个子协程。
        // 2、把挂起函数 try catch 起来，这样就能捕获 JobCancellationException 异常，然后就可以执行下面的 awaitClose 方法。
        awaitClose {
            onRemoveNotifyCallback()
            Log.d("BaseConnectExecutor", "通知监听被取消")
        }
    }

    final override fun close() {
        super.close()
        disconnect()
        ConnectExecutorFactory.remove(address)
    }

    override fun onBleOff() {
        super.onBleOff()
        // 在 connect 的时候关闭蓝牙开关，此时如果不调用 disconnect 方法，那么就不会清空回调（参考ConnectExecutor.onDisconnect），则会继续发送 connect 方法的相关错误，造成 UI 显示错乱。
        disconnect()
        // 如果蓝牙开关关闭后重新连接失败，那么可以重新扫描，然后再重新连接就能成功。但是这个要看外围设备是否支持，有的不需要重新扫描就能重连成功。
    }

    protected abstract fun onConnect(
        onDisconnectedListener: ((Throwable) -> Unit)?,
        onSuccess: ((List<BluetoothGattService>) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onDisconnect()

    protected abstract fun onReadCharacteristic(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        onSuccess: ((ByteArray) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onReadDescriptor(
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        onSuccess: ((ByteArray) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onReadRemoteRssi(
        onSuccess: ((Int) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onRequestConnectionPriority(
        connectionPriority: Int,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onRequestMtu(
        mtu: Int,
        onSuccess: ((Int) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onSetCharacteristicNotification(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        onSuccess: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    )

    protected abstract fun onWriteCharacteristic(
        data: ByteArray,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        writeType: Int,
        onSuccess: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    )

    protected abstract fun onWriteDescriptor(
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    )

    protected abstract fun onSetNotifyCallback(onResult: (ByteArray) -> Unit)

    protected abstract fun onRemoveNotifyCallback()

}
