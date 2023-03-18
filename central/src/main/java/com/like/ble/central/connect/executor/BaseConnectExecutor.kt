package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattService
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleException
import com.like.ble.util.MutexUtils
import com.like.ble.util.SuspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.*

/**
 * 蓝牙连接及数据操作的前提条件
 * 包括：并发处理、超时处理、蓝牙相关的前置条件检查、错误处理。
 */
class BaseConnectExecutor(activity: ComponentActivity, private val address: String?) : AbstractConnectExecutor(activity) {
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

    }

    final override fun disconnect() {
    }

    final override suspend fun readCharacteristic(characteristicUuid: UUID, serviceUuid: UUID?, timeout: Long): ByteArray? {

    }

    final override suspend fun readDescriptor(
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ): ByteArray? {

    }

    final override suspend fun setReadNotifyCallback(characteristicUuid: UUID, serviceUuid: UUID?) {

    }

    final override suspend fun readRemoteRssi(timeout: Long): Int {

    }

    final override suspend fun requestConnectionPriority(connectionPriority: Int, timeout: Long) {

    }

    final override suspend fun requestMtu(mtu: Int, timeout: Long): Int {

    }

    final override suspend fun setCharacteristicNotification(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        timeout: Long
    ) {

    }

    final override suspend fun writeCharacteristic(
        data: List<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    ) {

    }

    final override suspend fun writeDescriptor(
        data: List<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) {

    }

    final override fun close() {
        disconnect()
    }

}
