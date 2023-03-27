package com.like.ble.central.connect.executor

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import com.like.ble.callback.BleCallback
import com.like.ble.central.connect.callback.ConnectCallbackManager
import com.like.ble.exception.BleException
import com.like.ble.util.*
import kotlinx.coroutines.CancellableContinuation
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 蓝牙连接及数据操作的真正逻辑
 */
@SuppressLint("MissingPermission")
class ConnectExecutor(context: Context, address: String?) : BaseConnectExecutor(context, address) {
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mConnectCallbackManager: ConnectCallbackManager by lazy {
        ConnectCallbackManager()
    }

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    override fun onConnect(continuation: CancellableContinuation<List<BluetoothGattService>>, device: BluetoothDevice?) {
        var bluetoothDevice = device
        if (bluetoothDevice == null) {
            // 获取远端的蓝牙设备
            bluetoothDevice = mContext.getBluetoothAdapter()?.getRemoteDevice(address)
            if (bluetoothDevice == null) {
                continuation.resumeWithException(BleException("连接蓝牙失败：$address 未找到"))
                return
            }
        }
        mConnectCallbackManager.setConnectBleCallback(object : BleCallback<List<BluetoothGattService>>() {
            override fun onSuccess(data: List<BluetoothGattService>) {
                continuation.resume(data)
            }

            override fun onError(exception: BleException) {
                continuation.resumeWithException(exception)
            }
        })
        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothDevice.connectGatt(
                mContext,
                false,// 是否自动重连。不知道为什么，设置为true时会导致连接不上
                mConnectCallbackManager.getBluetoothGattCallback(),
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            bluetoothDevice.connectGatt(mContext, false, mConnectCallbackManager.getBluetoothGattCallback())
        }
    }

    override fun onDisconnect() {
        if (mContext.isBleDeviceConnected(mBluetoothGatt?.device)) {
            mBluetoothGatt?.disconnect()
        }
        // close()时会清空BluetoothGatt内部的mCallback回调。导致收不到断开连接的消息。所以就不能在断开连接状态回调时处理 UI。
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    override fun onReadCharacteristic(
        continuation: CancellableContinuation<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID?
    ) {
        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            continuation.resumeWithException(BleException("特征值不存在：${characteristicUuid.getValidString()}"))
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            continuation.resumeWithException(BleException("this characteristic not support read!"))
            return
        }

        mConnectCallbackManager.setReadCharacteristicBleCallback(object : BleCallback<ByteArray>() {
            override fun onSuccess(data: ByteArray) {
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
        continuation: CancellableContinuation<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?
    ) {
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

        mConnectCallbackManager.setReadDescriptorBleCallback(object : BleCallback<ByteArray>() {
            override fun onSuccess(data: ByteArray) {
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

    override fun onReadRemoteRssi(continuation: CancellableContinuation<Int>) {
        mConnectCallbackManager.setReadRemoteRssiBleCallback(object : BleCallback<Int>() {
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

    override fun onRequestConnectionPriority(continuation: CancellableContinuation<Unit>, connectionPriority: Int) {
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

    override fun onRequestMtu(continuation: CancellableContinuation<Int>, mtu: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            continuation.resumeWithException(BleException("android 5.0及其以上才支持设置MTU：$address"))
            return
        }

        mConnectCallbackManager.setRequestMtuBleCallback(object : BleCallback<Int>() {
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
        enable: Boolean
    ) {
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
        writeType: Int
    ) {
        if (data.isEmpty()) {
            continuation.resumeWithException(BleException("data is empty"))
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

        mConnectCallbackManager.setWriteCharacteristicBleCallback(object : BleCallback<Unit>() {
            override fun onSuccess(data: Unit) {
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
        serviceUuid: UUID?
    ) {
        if (data.isEmpty()) {
            continuation.resumeWithException(BleException("data is empty"))
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

        mConnectCallbackManager.setWriteDescriptorBleCallback(object : BleCallback<Unit>() {
            override fun onSuccess(data: Unit) {
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

    override fun onSetNotifyCallback(characteristicUuid: UUID, onResult: (ByteArray) -> Unit) {
        mConnectCallbackManager.setReadNotifyBleCallback(characteristicUuid, object : BleCallback<ByteArray>() {
            override fun onSuccess(data: ByteArray) {
                onResult(data)
            }
        })
    }

    override fun onRemoveNotifyCallback(characteristicUuid: UUID) {
        mConnectCallbackManager.setReadNotifyBleCallback(characteristicUuid, null)
    }

}