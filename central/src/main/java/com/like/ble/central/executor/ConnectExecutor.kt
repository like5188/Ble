package com.like.ble.central.executor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.central.util.PermissionUtils
import com.like.ble.exception.BleException
import com.like.ble.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙连接及连接成功后的命令执行者
 * 可以进行连接、断开连接、操作数据等等操作
 */
@SuppressLint("MissingPermission")
class ConnectExecutor(private val activity: ComponentActivity) : IConnectExecutor {
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mConnectCallbackManager: ConnectCallbackManager by lazy {
        ConnectCallbackManager()
    }
    private val _notifyFlow: MutableSharedFlow<ByteArray?> by lazy {
        MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
    }
    override val notifyFlow: Flow<ByteArray?> = _notifyFlow

    override suspend fun connect(address: String, timeout: Long): List<BluetoothGattService>? {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity, false)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (activity.isBleDeviceConnected(mBluetoothGatt?.device)) return mBluetoothGatt?.services
        // 获取远端的蓝牙设备
        val bluetoothDevice = activity.getBluetoothAdapter()?.getRemoteDevice(address) ?: throw BleException("连接蓝牙失败：$address 未找到")
        return suspendCancellableCoroutineWithTimeout(timeout, "连接蓝牙设备超时：$address") { continuation ->
            continuation.invokeOnCancellation {
                disconnect()
            }
            mConnectCallbackManager.connectCallback = object : ConnectCallback {
                override fun onSuccess(services: List<BluetoothGattService>?) {
                    continuation.resume(services)
                }

                override fun onError(exception: BleException) {
                    disconnect()
                    continuation.resumeWithException(exception)
                }

            }
            mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothDevice.connectGatt(
                    activity,
                    false,
                    mConnectCallbackManager.gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )// 第二个参数表示是否自动重连
            } else {
                bluetoothDevice.connectGatt(activity, false, mConnectCallbackManager.gattCallback)// 第二个参数表示是否自动重连
            }
        }
    }

    override fun disconnect() {
        if (!activity.isBluetoothEnable()) {
            return
        }
        if (!PermissionUtils.checkPermissions(activity, false)) {
            return
        }
        if (activity.isBleDeviceConnected(mBluetoothGatt?.device)) {
            mBluetoothGatt?.disconnect()
        }
        // 这里的close()方法会清空BluetoothGatt中的mGattCallback，导致收不到回调，但是可以使用本地缓存的各个命令来回调
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    override suspend fun readCharacteristic(address: String, characteristicUuid: UUID, serviceUuid: UUID?, timeout: Long): ByteArray? {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity, false)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!activity.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            throw BleException("this characteristic not support read!")
        }

        return suspendCancellableCoroutineWithTimeout(timeout, "读取特征值超时：${characteristicUuid.getValidString()}") { continuation ->
            mConnectCallbackManager.readCharacteristicCallback = object : ReadCharacteristicCallback {
                override fun onSuccess(data: ByteArray?) {
                    continuation.resume(data)
                }

                override fun onError(exception: BleException) {
                    continuation.resumeWithException(exception)
                }
            }
            if (mBluetoothGatt?.readCharacteristic(characteristic) != true) {
                continuation.resumeWithException(BleException("读取特征值失败：${characteristicUuid.getValidString()}"))
            }
        }
    }

    override suspend fun readDescriptor(
        address: String,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ): ByteArray? {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity, false)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!activity.isBleDeviceConnected(mBluetoothGatt?.device)) {
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
            mConnectCallbackManager.readDescriptorCallback = object : ReadDescriptorCallback {
                override fun onSuccess(data: ByteArray?) {
                    continuation.resume(data)
                }

                override fun onError(exception: BleException) {
                    continuation.resumeWithException(exception)
                }
            }
            if (mBluetoothGatt?.readDescriptor(descriptor) != true) {
                continuation.resumeWithException(BleException("读取描述失败：${descriptorUuid.getValidString()}"))
            }
        }
    }

    override suspend fun readNotify(address: String, characteristicUuid: UUID, serviceUuid: UUID?) {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity, false)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (!activity.isBleDeviceConnected(mBluetoothGatt?.device)) {
            throw BleException("蓝牙未连接：$address")
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_INDICATE or BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            throw BleException("this characteristic not support indicate or notify!")
        }

        mConnectCallbackManager.readNotifyCallback = object : ReadNotifyCallback {
            override fun onSuccess(data: ByteArray?) {
                _notifyFlow.tryEmit(data)
            }

            override fun onError(exception: BleException) {
            }
        }
    }

    override suspend fun close() {
        disconnect()
    }
}