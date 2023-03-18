package com.like.ble.central.connect.executor

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.callback.BleCallback
import com.like.ble.central.connect.callback.ByteArrayCallback
import com.like.ble.central.connect.callback.ConnectCallback
import com.like.ble.central.connect.callback.ConnectCallbackManager
import com.like.ble.central.connect.callback.IntCallback
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionDeviceDisconnected
import com.like.ble.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙连接及数据操作的真正逻辑
 */
@SuppressLint("MissingPermission")
class ConnectExecutor(activity: ComponentActivity, private val address: String?) : AbstractConnectExecutor(activity) {
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mConnectCallbackManager: ConnectCallbackManager by lazy {
        ConnectCallbackManager()
    }
    private val suspendCancellableCoroutineWithTimeout by lazy {
        SuspendCancellableCoroutineWithTimeout()
    }
    private val _notifyFlow: MutableSharedFlow<ByteArray?> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    override val notifyFlow: Flow<ByteArray?> = _notifyFlow

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    override suspend fun connect(timeout: Long): List<BluetoothGattService>? = withContext(Dispatchers.IO) {
        checkEnvironmentOrThrow()
        if (context.isBleDeviceConnected(mBluetoothGatt?.device)) return@withContext mBluetoothGatt?.services
        // 获取远端的蓝牙设备
        val bluetoothDevice = context.getBluetoothAdapter()?.getRemoteDevice(address) ?: throw BleException("连接蓝牙失败：$address 未找到")
        suspendCancellableCoroutineWithTimeout.execute(timeout, "连接蓝牙设备超时：$address") { continuation ->
            mConnectCallbackManager.setConnectCallback(object : ConnectCallback() {
                override fun onSuccess(services: List<BluetoothGattService>?) {
                    continuation.resume(services)
                }

                override fun onError(exception: BleException) {
                    disconnect()
                    continuation.resumeWithException(exception)
                }

            })
            mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothDevice.connectGatt(
                    context,
                    false,// 不知道为什么，设置为true时会导致连接不上
                    mConnectCallbackManager.getBluetoothGattCallback(),
                    BluetoothDevice.TRANSPORT_LE
                )// 第二个参数表示是否自动重连
            } else {
                bluetoothDevice.connectGatt(context, false, mConnectCallbackManager.getBluetoothGattCallback())// 第二个参数表示是否自动重连
            }
        }
    }

    override fun disconnect() {
        if (!checkEnvironment()) {
            return
        }
        // 此处如果不取消，那么还会把超时错误传递出去的。
        suspendCancellableCoroutineWithTimeout.cancel()
        // close()时会清空BluetoothGatt内部的mCallback回调。导致收不到断开连接的消息。
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    override suspend fun readCharacteristic(characteristicUuid: UUID, serviceUuid: UUID?, timeout: Long): ByteArray? =
        withContext(Dispatchers.IO) {
            checkEnvironmentOrThrow()
            if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
                throw BleExceptionDeviceDisconnected(address)
            }

            val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
                ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
                throw BleException("this characteristic not support read!")
            }

            suspendCancellableCoroutineWithTimeout.execute(timeout, "读取特征值超时：${characteristicUuid.getValidString()}") { continuation ->
                mConnectCallbackManager.setReadCharacteristicCallback(object : ByteArrayCallback() {
                    override fun onSuccess(data: ByteArray?) {
                        continuation.resume(data)
                    }

                    override fun onError(exception: BleException) {
                        continuation.resumeWithException(exception)
                    }
                })
                if (mBluetoothGatt?.readCharacteristic(characteristic) != true) {
                    continuation.resumeWithException(BleException("读取特征值失败：${characteristicUuid.getValidString()}"))
                }
            }
        }

    override suspend fun readDescriptor(
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ): ByteArray? = withContext(Dispatchers.IO) {
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        val descriptor = mBluetoothGatt?.findDescriptor(
            descriptorUuid,
            characteristicUuid,
            serviceUuid
        ) ?: throw BleException("描述不存在：${descriptorUuid.getValidString()}")

        // 由于descriptor.permissions永远为0x0000，所以无法判断，但是如果权限不允许，还是会操作失败的。
//        if (descriptor.permissions and (BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) == 0) {
//            command.error("this descriptor not support read!")
//            return
//        }

        suspendCancellableCoroutineWithTimeout.execute(timeout, "读取描述值超时：${descriptorUuid.getValidString()}") { continuation ->
            mConnectCallbackManager.setReadDescriptorCallback(object : ByteArrayCallback() {
                override fun onSuccess(data: ByteArray?) {
                    continuation.resume(data)
                }

                override fun onError(exception: BleException) {
                    continuation.resumeWithException(exception)
                }
            })
            if (mBluetoothGatt?.readDescriptor(descriptor) != true) {
                continuation.resumeWithException(BleException("读取描述失败：${descriptorUuid.getValidString()}"))
            }
        }
    }

    override suspend fun setReadNotifyCallback(characteristicUuid: UUID, serviceUuid: UUID?) = withContext(Dispatchers.IO) {
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_INDICATE or BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            throw BleException("this characteristic not support indicate or notify!")
        }

        mConnectCallbackManager.setReadNotifyCallback(object : ByteArrayCallback() {
            override fun onSuccess(data: ByteArray?) {
                _notifyFlow.tryEmit(data)
            }
        })
    }

    override suspend fun readRemoteRssi(timeout: Long): Int = withContext(Dispatchers.IO) {
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        suspendCancellableCoroutineWithTimeout.execute(timeout, "读RSSI超时：$address") { continuation ->
            mConnectCallbackManager.setReadRemoteRssiCallback(object : IntCallback() {
                override fun onSuccess(data: Int) {
                    continuation.resume(data)
                }

                override fun onError(exception: BleException) {
                    continuation.resumeWithException(exception)
                }
            })

            if (mBluetoothGatt?.readRemoteRssi() != true) {
                continuation.resumeWithException(BleException("读RSSI失败：$address"))
            }
        }
    }

    override suspend fun requestConnectionPriority(connectionPriority: Int, timeout: Long) = withContext(Dispatchers.IO) {
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw BleException("android 5.0及其以上才支持requestConnectionPriority：$address")
        }

        suspendCancellableCoroutineWithTimeout.execute(timeout, "设置ConnectionPriority超时：$address") { continuation ->
            if (mBluetoothGatt?.requestConnectionPriority(connectionPriority) != true) {
                continuation.resumeWithException(BleException("设置ConnectionPriority失败：$address"))
            }
            continuation.resume(Unit)
        }
    }

    override suspend fun requestMtu(mtu: Int, timeout: Long): Int = withContext(Dispatchers.IO) {
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw BleException("android 5.0及其以上才支持设置MTU：$address")
        }

        suspendCancellableCoroutineWithTimeout.execute(timeout, "设置MTU超时：$address") { continuation ->
            mConnectCallbackManager.setRequestMtuCallback(object : IntCallback() {
                override fun onSuccess(data: Int) {
                    continuation.resume(data)
                }

                override fun onError(exception: BleException) {
                    continuation.resumeWithException(exception)
                }
            })

            if (mBluetoothGatt?.requestMtu(mtu) != true) {
                continuation.resumeWithException(BleException("设置MTU失败：$address"))
            }
        }
    }

    override suspend fun setCharacteristicNotification(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        timeout: Long
    ) = withContext(Dispatchers.IO) {
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            throw BleException("this characteristic not support notify or indicate")
        }

        // cccd : clinet characteristic configuration descriptor
        // 服务端一开始是无法直接发送Indication和Notification。
        // 首先必须是客户端通过往服务端的CCCD特征（clinet characteristic configuration descriptor）
        // 写入值来使能服务端的这两个功能Notification/Indication，这样服务端才能发送。
        val cccd = characteristic.getDescriptor(createBleUuidBy16Bit("2902"))
            ?: throw BleException("getDescriptor fail")

        cccd.value = when (type) {
            0 -> {
                if (enable) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
            }
            1 -> {
                if (enable) {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
            }
            else -> throw BleException("type can only be 0 or 1")
        }

        suspendCancellableCoroutineWithTimeout.execute(timeout, "设置通知超时：$address") { continuation ->
            if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
                continuation.resumeWithException(BleException("setCharacteristicNotification fail"))
            }
            if (mBluetoothGatt?.writeDescriptor(cccd) != true) {
                continuation.resumeWithException(BleException("writeDescriptor fail"))
            }
            continuation.resume(Unit)
        }

    }

    override suspend fun writeCharacteristic(
        data: List<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    ) = withContext(Dispatchers.IO) {
        if (data.isEmpty()) {
            throw BleException("data is empty")
        }
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            throw BleException("this characteristic not support write!")
        }

        suspendCancellableCoroutineWithTimeout.execute(timeout, "写特征值超时：${characteristicUuid.getValidString()}") { continuation ->
            // 是否可以进行下一批次的写入操作
            val nextFlag = AtomicBoolean(false)
            mConnectCallbackManager.setWriteCharacteristicCallback(object : BleCallback() {
                override fun onSuccess() {
                    nextFlag.set(true)
                }

                override fun onError(exception: BleException) {
                    continuation.resumeWithException(exception)
                }
            })
            /*
                写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
                WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
                WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
                WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
            */
            characteristic.writeType = writeType
            launch {
                data.forEach {
                    characteristic.value = it
                    if (mBluetoothGatt?.writeCharacteristic(characteristic) != true) {
                        continuation.resumeWithException(BleException("写特征值失败：${characteristicUuid.getValidString()}"))
                    }
                    do {
                        delay(20)
                    } while (!nextFlag.get())
                    nextFlag.set(false)
                }
                continuation.resume(Unit)
            }
        }

    }

    override suspend fun writeDescriptor(
        data: List<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) = withContext(Dispatchers.IO) {
        if (data.isEmpty()) {
            throw BleException("data is empty")
        }
        checkEnvironmentOrThrow()
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleExceptionDeviceDisconnected(address)
        }

        val descriptor = mBluetoothGatt?.findDescriptor(descriptorUuid, characteristicUuid, serviceUuid)
            ?: throw BleException("描述值不存在：${descriptorUuid.getValidString()}")

        // 由于descriptor.permissions永远为0x0000，所以无法判断，但是如果权限不允许，还是会操作失败的。
//        if (descriptor.permissions and
//            (BluetoothGattDescriptor.PERMISSION_WRITE or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED or
//                    BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM
//                    ) == 0
//        ) {
//            command.error("this descriptor not support write!")
//            return
//        }

        suspendCancellableCoroutineWithTimeout.execute(timeout, "写描述值超时：${descriptorUuid.getValidString()}") { continuation ->
            // 是否可以进行下一批次的写入操作
            val nextFlag = AtomicBoolean(false)
            mConnectCallbackManager.setWriteDescriptorCallback(object : BleCallback() {
                override fun onSuccess() {
                    nextFlag.set(true)
                }

                override fun onError(exception: BleException) {
                    continuation.resumeWithException(exception)
                }
            })

            launch {
                data.forEach {
                    descriptor.value = it
                    if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
                        continuation.resumeWithException(BleException("写描述值失败：${descriptorUuid.getValidString()}"))
                    }
                    do {
                        delay(20)
                    } while (!nextFlag.get())
                    nextFlag.set(false)
                }
                continuation.resume(Unit)
            }
        }
    }

    override fun close() {
        disconnect()
    }
}