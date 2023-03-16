package com.like.ble.central.connect.executor

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.callback.BleCallback
import com.like.ble.central.connect.PermissionUtils
import com.like.ble.central.connect.callback.ByteArrayCallback
import com.like.ble.central.connect.callback.ConnectCallback
import com.like.ble.central.connect.callback.ConnectCallbackManager
import com.like.ble.central.connect.callback.IntCallback
import com.like.ble.exception.BleException
import com.like.ble.util.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withTimeout
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙连接及连接成功后的命令执行者
 * 可以进行连接、断开连接、操作数据等等操作
 */
@SuppressLint("MissingPermission")
class ConnectExecutor(private val activity: ComponentActivity, private val address: String?) : IConnectExecutor {
    private val context = activity.applicationContext
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mConnectCallbackManager: ConnectCallbackManager by lazy {
        ConnectCallbackManager()
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

    override suspend fun connect(timeout: Long): List<BluetoothGattService>? {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (context.isBleDeviceConnected(mBluetoothGatt?.device)) return mBluetoothGatt?.services
        // 获取远端的蓝牙设备
        val bluetoothDevice = context.getBluetoothAdapter()?.getRemoteDevice(address) ?: throw BleException("连接蓝牙失败：$address 未找到")
        return suspendCancellableCoroutineWithTimeout(timeout, "连接蓝牙设备超时：$address") { continuation ->
            continuation.invokeOnCancellation {
                disconnect()
            }
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
                    false,
                    mConnectCallbackManager.getBluetoothGattCallback(),
                    BluetoothDevice.TRANSPORT_LE
                )// 第二个参数表示是否自动重连
            } else {
                bluetoothDevice.connectGatt(context, false, mConnectCallbackManager.getBluetoothGattCallback())// 第二个参数表示是否自动重连
            }
        }
    }

    override fun disconnect() {
        if (!context.isBluetoothEnable()) {
            return
        }
        if (!PermissionUtils.checkPermissions(context)) {
            return
        }
        if (context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            mBluetoothGatt?.disconnect()
        }
        // 这里的close()方法会清空BluetoothGatt中的mGattCallback，导致收不到回调，但是可以使用本地缓存的各个命令来回调
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    override suspend fun readCharacteristic(characteristicUuid: UUID, serviceUuid: UUID?, timeout: Long): ByteArray? {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            throw BleException("this characteristic not support read!")
        }

        return suspendCancellableCoroutineWithTimeout(timeout, "读取特征值超时：${characteristicUuid.getValidString()}") { continuation ->
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
    ): ByteArray? {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
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

        return suspendCancellableCoroutineWithTimeout(timeout, "读取描述值超时：${descriptorUuid.getValidString()}") { continuation ->
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

    override suspend fun readNotify(characteristicUuid: UUID, serviceUuid: UUID?) {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
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

    override suspend fun readRemoteRssi(timeout: Long): Int {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        return suspendCancellableCoroutineWithTimeout(timeout, "读RSSI超时：$address") { continuation ->
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

    override suspend fun requestConnectionPriority(connectionPriority: Int): Boolean {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw BleException("android 5.0及其以上才支持requestConnectionPriority：$address")
        }

        return mBluetoothGatt?.requestConnectionPriority(connectionPriority) ?: false
    }

    override suspend fun requestMtu(mtu: Int, timeout: Long): Int {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw BleException("android 5.0及其以上才支持设置MTU：$address")
        }

        return suspendCancellableCoroutineWithTimeout(timeout, "设置MTU超时：$address") { continuation ->
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
        enable: Boolean
    ): Boolean {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            throw BleException("this characteristic not support notify or indicate")
        }
        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            throw BleException("setCharacteristicNotification fail")
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
            else -> return false
        }
        return mBluetoothGatt?.writeDescriptor(cccd) ?: false
    }

    override suspend fun writeCharacteristic(
        data: List<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    ) {
        if (data.isEmpty()) {
            throw BleException("data is empty")
        }
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            throw BleException("this characteristic not support write!")
        }

        try {
            withTimeout(timeout) {
                // 是否可以进行下一批次的写入操作
                val nextFlag = AtomicBoolean(false)
                mConnectCallbackManager.setWriteCharacteristicCallback(object : BleCallback() {
                    override fun onSuccess() {
                        nextFlag.set(true)
                    }

                    override fun onError(exception: BleException) {
                        throw exception
                    }
                })
                /*
                    写特征值前可以设置写的类型setWriteType()，写类型有三种，如下：
                    WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
                    WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
                    WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
                */
                characteristic.writeType = writeType
                data.forEach {
                    characteristic.value = it
                    if (mBluetoothGatt?.writeCharacteristic(characteristic) != true) {
                        throw BleException("写特征值失败：${characteristicUuid.getValidString()}")
                    }
                    do {
                        delay(20)
                    } while (!nextFlag.get())
                    nextFlag.set(false)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw BleException("写特征值超时：${characteristicUuid.getValidString()}")
        }

    }

    override suspend fun writeDescriptor(
        data: List<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) {
        if (data.isEmpty()) {
            throw BleException("data is empty")
        }
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
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

        try {
            withTimeout(timeout) {
                // 是否可以进行下一批次的写入操作
                val nextFlag = AtomicBoolean(false)
                mConnectCallbackManager.setWriteDescriptorCallback(object : BleCallback() {
                    override fun onSuccess() {
                        nextFlag.set(true)
                    }

                    override fun onError(exception: BleException) {
                        throw exception
                    }
                })

                data.forEach {
                    descriptor.value = it
                    if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
                        throw BleException("写描述值失败：${descriptorUuid.getValidString()}")
                    }
                    do {
                        delay(20)
                    } while (!nextFlag.get())
                    nextFlag.set(false)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw BleException("写描述值超时：${descriptorUuid.getValidString()}")
        }

    }

    override fun close() {
        disconnect()
    }
}