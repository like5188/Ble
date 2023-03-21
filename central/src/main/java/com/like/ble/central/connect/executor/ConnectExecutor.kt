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
import com.like.ble.central.connect.result.ConnectResult
import com.like.ble.exception.BleException
import com.like.ble.exception.BleExceptionBusy
import com.like.ble.exception.BleExceptionDeviceDisconnected
import com.like.ble.util.*
import kotlinx.coroutines.CancellableContinuation
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙连接及数据操作的真正逻辑
 */
@SuppressLint("MissingPermission")
class ConnectExecutor(activity: ComponentActivity, address: String?) : BaseConnectExecutor(activity, address) {
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mConnectCallbackManager: ConnectCallbackManager by lazy {
        ConnectCallbackManager()
    }

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    override fun onConnect(continuation: CancellableContinuation<List<BluetoothGattService>?>, timeout: Long) {
        if (context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionBusy("设备已经连接"))
            return
        }
        _connectFlow.tryEmit(ConnectResult.Ready)
        // 获取远端的蓝牙设备
        val bluetoothDevice = context.getBluetoothAdapter()?.getRemoteDevice(address)
        if (bluetoothDevice == null) {
            continuation.resumeWithException(BleException("连接蓝牙失败：$address 未找到"))
            return
        }
        mConnectCallbackManager.setConnectCallback(object : ConnectCallback() {
            override fun onSuccess(services: List<BluetoothGattService>?) {
                continuation.resume(services)
            }

            override fun onError(exception: BleException) {
                // 因为在第一次 resumeWithException 后，BaseConnectExecutor 的 connect 方法就执行完毕了，continuation.isActive == false 了。
                // 那么在后续的蓝牙连接状态改变后，就不能再 resumeWithException 了。
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                } else {
                    // 保证蓝牙中途断开能发射
                    _connectFlow.tryEmit(ConnectResult.Error(exception))
                }
            }

        })
        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice.connectGatt(
                context,
                false,// 是否自动重连。不知道为什么，设置为true时会导致连接不上
                mConnectCallbackManager.getBluetoothGattCallback(),
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            bluetoothDevice.connectGatt(context, false, mConnectCallbackManager.getBluetoothGattCallback())
        }
    }

    override fun onDisconnect() {
        // close()时会清空BluetoothGatt内部的mCallback回调。导致收不到断开连接的消息。所以就不能在断开连接状态回调时处理 UI。
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    override fun onReadCharacteristic(
        continuation: CancellableContinuation<ByteArray?>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long
    ) {
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            continuation.resumeWithException(BleException("特征值不存在：${characteristicUuid.getValidString()}"))
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            continuation.resumeWithException(BleException("this characteristic not support read!"))
            return
        }

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

    override fun onReadDescriptor(
        continuation: CancellableContinuation<ByteArray?>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) {
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        val descriptor = mBluetoothGatt?.findDescriptor(
            descriptorUuid,
            characteristicUuid,
            serviceUuid
        )
        if (descriptor == null) {
            continuation.resumeWithException(BleException("描述不存在：${descriptorUuid.getValidString()}"))
            return
        }

        // 由于descriptor.permissions永远为0x0000，所以无法判断，但是如果权限不允许，还是会操作失败的。
//        if (descriptor.permissions and (BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) == 0) {
//            command.error("this descriptor not support read!")
//            return
//        }

        mConnectCallbackManager.setReadDescriptorCallback(object : ByteArrayCallback() {
            override fun onSuccess(data: ByteArray?) {
                continuation.resume(data)
            }

            override fun onError(exception: BleException) {
                continuation.resumeWithException(exception)
            }
        })
        if (mBluetoothGatt?.readDescriptor(descriptor) != true) {
            continuation.resumeWithException(BleException("读取描述值失败：${descriptorUuid.getValidString()}"))
        }
    }

    override fun onSetReadNotifyCallback(characteristicUuid: UUID, serviceUuid: UUID?) {
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

    override fun onReadRemoteRssi(continuation: CancellableContinuation<Int>, timeout: Long) {
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        mConnectCallbackManager.setReadRemoteRssiCallback(object : IntCallback() {
            override fun onSuccess(data: Int) {
                continuation.resume(data)
            }

            override fun onError(exception: BleException) {
                continuation.resumeWithException(exception)
            }
        })

        if (mBluetoothGatt?.readRemoteRssi() != true) {
            continuation.resumeWithException(BleException("读取RSSI失败：$address"))
        }
    }

    override fun onRequestConnectionPriority(continuation: CancellableContinuation<Unit>, connectionPriority: Int, timeout: Long) {
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            continuation.resumeWithException(BleException("android 5.0及其以上才支持requestConnectionPriority：$address"))
            return
        }

        if (mBluetoothGatt?.requestConnectionPriority(connectionPriority) != true) {
            continuation.resumeWithException(BleException("设置ConnectionPriority失败：$address"))
            return
        }
        continuation.resume(Unit)
    }

    override fun onRequestMtu(continuation: CancellableContinuation<Int>, mtu: Int, timeout: Long) {
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            continuation.resumeWithException(BleException("android 5.0及其以上才支持设置MTU：$address"))
            return
        }

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

    override fun onSetCharacteristicNotification(
        continuation: CancellableContinuation<Unit>,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        timeout: Long
    ) {
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            continuation.resumeWithException(BleException("特征值不存在：${characteristicUuid.getValidString()}"))
            return
        }

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            continuation.resumeWithException(BleException("this characteristic not support notify or indicate"))
            return
        }

        // cccd : clinet characteristic configuration descriptor
        // 服务端一开始是无法直接发送Indication和Notification。
        // 首先必须是客户端通过往服务端的CCCD特征（clinet characteristic configuration descriptor）
        // 写入值来使能服务端的这两个功能Notification/Indication，这样服务端才能发送。
        val cccd = characteristic.getDescriptor(createBleUuidBy16Bit("2902"))
        if (cccd == null) {
            continuation.resumeWithException(BleException("getDescriptor fail"))
            return
        }

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
            else -> {
                continuation.resumeWithException(BleException("type can only be 0 or 1"))
                return
            }
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            continuation.resumeWithException(BleException("setCharacteristicNotification fail"))
            return
        }
        if (mBluetoothGatt?.writeDescriptor(cccd) != true) {
            continuation.resumeWithException(BleException("writeDescriptor fail"))
            return
        }
        continuation.resume(Unit)
    }

    override fun onWriteCharacteristic(
        continuation: CancellableContinuation<Unit>,
        data: ByteArray,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        timeout: Long,
        writeType: Int
    ) {
        if (data.isEmpty()) {
            continuation.resumeWithException(BleException("data is empty"))
            return
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            continuation.resumeWithException(BleException("特征值不存在：${characteristicUuid.getValidString()}"))
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            continuation.resumeWithException(BleException("this characteristic not support write!"))
            return
        }

        mConnectCallbackManager.setWriteCharacteristicCallback(object : BleCallback() {
            override fun onSuccess() {
                continuation.resume(Unit)
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
        characteristic.value = data
        if (mBluetoothGatt?.writeCharacteristic(characteristic) != true) {
            continuation.resumeWithException(BleException("写特征值失败：${characteristicUuid.getValidString()}"))
        }
    }

    override fun onWriteDescriptor(
        continuation: CancellableContinuation<Unit>,
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        timeout: Long
    ) {
        if (data.isEmpty()) {
            continuation.resumeWithException(BleException("data is empty"))
            return
        }
        if (!context.isBleDeviceConnected(mBluetoothGatt?.device)) {
            continuation.resumeWithException(BleExceptionDeviceDisconnected(address))
            return
        }

        val descriptor = mBluetoothGatt?.findDescriptor(descriptorUuid, characteristicUuid, serviceUuid)
        if (descriptor == null) {
            continuation.resumeWithException(BleException("描述不存在：${descriptorUuid.getValidString()}"))
            return
        }

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

        mConnectCallbackManager.setWriteDescriptorCallback(object : BleCallback() {
            override fun onSuccess() {
                continuation.resume(Unit)
            }

            override fun onError(exception: BleException) {
                continuation.resumeWithException(exception)
            }
        })

        descriptor.value = data
        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            continuation.resumeWithException(BleException("写描述值失败：${descriptorUuid.getValidString()}"))
        }
    }
}