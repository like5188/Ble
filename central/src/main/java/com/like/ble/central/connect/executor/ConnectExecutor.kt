package com.like.ble.central.connect.executor

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import com.like.ble.callback.BleCallback
import com.like.ble.central.connect.callback.ConnectCallbackManager
import com.like.ble.exception.BleException
import com.like.ble.util.*
import java.util.*

/**
 * 蓝牙连接及数据操作的真正逻辑
 */
@SuppressLint("MissingPermission")
internal class ConnectExecutor(context: Context, address: String?) : BaseConnectExecutor(context, address) {
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mConnectCallbackManager: ConnectCallbackManager by lazy {
        ConnectCallbackManager(context)
    }

    init {
        if (address.isNullOrEmpty() || !BluetoothAdapter.checkBluetoothAddress(address)) {
            throw BleException("invalid address：$address")
        }
    }

    override fun onConnect(
        onSuccess: ((BluetoothDevice, List<BluetoothGattService>) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val device = mContext.getBluetoothAdapter()?.getRemoteDevice(address)
        if (device == null) {
            onError?.invoke(BleException("连接蓝牙失败，获取蓝牙设备失败：$address"))
            return
        }
        mConnectCallbackManager.setConnectBleCallback(object : BleCallback<List<BluetoothGattService>>() {
            override fun onSuccess(data: List<BluetoothGattService>) {
                onSuccess?.invoke(device, data)
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
            }
        })
        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(
                mContext,
                false,// 参数autoConnect，如果为 true 的话，系统就会发起一个后台连接，等到系统发现了一个设备，就会自动连上，通常这个过程是非常慢的。为 false 的话，就会直接连接，通常会比较快。同样，BluetoothGatt.connect()只能发起一个后台连接，不是直接连接。所以连接时设置autoConnect参数设置为false，如果想实现重连功能的话，最好自己去手动实现。
                mConnectCallbackManager.getBluetoothGattCallback(),
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            device.connectGatt(mContext, false, mConnectCallbackManager.getBluetoothGattCallback())
        }
    }

    override fun onDisconnect() {
        val gatt = mBluetoothGatt ?: return
        mBluetoothGatt = null
        if (mContext.isBleDeviceConnected(gatt.device)) {
            gatt.disconnect()
        } else {
            gatt.refreshDeviceCache()
            // close()时会清空BluetoothGatt内部的mCallback回调。导致不能收到onConnectionStateChange断开连接的回调。所以就不能在此回调中处理 UI。需要直接处理。
            gatt.close()
        }
    }

    override fun onReadCharacteristic(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        onSuccess: ((ByteArray) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            onError?.invoke(BleException("特征值不存在：${characteristicUuid.getValidString()}"))
            return
        }

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            onError?.invoke(BleException("this characteristic not support read!"))
            return
        }

        mConnectCallbackManager.setReadCharacteristicBleCallback(object : BleCallback<ByteArray>() {
            override fun onSuccess(data: ByteArray) {
                onSuccess?.invoke(data)
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
            }
        })
        if (mBluetoothGatt?.readCharacteristic(characteristic) != true) {
            onError?.invoke(BleException("读取特征值失败：${characteristicUuid.getValidString()}"))
        }
    }

    override fun onReadDescriptor(
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        onSuccess: ((ByteArray) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val descriptor = mBluetoothGatt?.findDescriptor(
            descriptorUuid,
            characteristicUuid,
            serviceUuid
        )
        if (descriptor == null) {
            onError?.invoke(BleException("描述不存在：${descriptorUuid.getValidString()}"))
            return
        }

        // 由于descriptor.permissions永远为0x0000，所以无法判断，但是如果权限不允许，还是会操作失败的。
//        if (descriptor.permissions and (BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) == 0) {
//            command.error("this descriptor not support read!")
//            return
//        }

        mConnectCallbackManager.setReadDescriptorBleCallback(object : BleCallback<ByteArray>() {
            override fun onSuccess(data: ByteArray) {
                onSuccess?.invoke(data)
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
            }
        })
        if (mBluetoothGatt?.readDescriptor(descriptor) != true) {
            onError?.invoke(BleException("读取描述值失败：${descriptorUuid.getValidString()}"))
        }
    }

    override fun onReadRemoteRssi(
        onSuccess: ((Int) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        mConnectCallbackManager.setReadRemoteRssiBleCallback(object : BleCallback<Int>() {
            override fun onSuccess(data: Int) {
                onSuccess?.invoke(data)
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
            }
        })

        if (mBluetoothGatt?.readRemoteRssi() != true) {
            onError?.invoke(BleException("读取RSSI失败：$address"))
        }
    }

    override fun onRequestConnectionPriority(
        connectionPriority: Int,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onError?.invoke(BleException("android 5.0及其以上才支持requestConnectionPriority：$address"))
            return
        }

        if (mBluetoothGatt?.requestConnectionPriority(connectionPriority) != true) {
            onError?.invoke(BleException("设置ConnectionPriority失败：$address"))
            return
        }
        onSuccess?.invoke()
    }

    override fun onRequestMtu(
        mtu: Int,
        onSuccess: ((Int) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onError?.invoke(BleException("android 5.0及其以上才支持设置MTU：$address"))
            return
        }

        mConnectCallbackManager.setRequestMtuBleCallback(object : BleCallback<Int>() {
            override fun onSuccess(data: Int) {
                onSuccess?.invoke(data)
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
            }
        })

        if (mBluetoothGatt?.requestMtu(mtu) != true) {
            onError?.invoke(BleException("设置MTU失败：$address"))
        }
    }

    override fun onSetCharacteristicNotification(
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        type: Int,
        enable: Boolean,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            onError?.invoke(BleException("特征值不存在：${characteristicUuid.getValidString()}"))
            return
        }

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            onError?.invoke(BleException("this characteristic not support notify or indicate"))
            return
        }

        // cccd : clinet characteristic configuration descriptor
        // 服务端一开始是无法直接发送Indication和Notification。
        // 首先必须是客户端通过往服务端的CCCD特征（clinet characteristic configuration descriptor）
        // 写入值来使能服务端的这两个功能Notification/Indication，这样服务端才能发送。
        val cccd = characteristic.getDescriptor(createBleUuidBy16Bit("2902"))
        if (cccd == null) {
            onError?.invoke(BleException("getDescriptor fail"))
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
                onError?.invoke(BleException("type can only be 0 or 1"))
                return
            }
        }

        if (mBluetoothGatt?.setCharacteristicNotification(characteristic, enable) != true) {
            onError?.invoke(BleException("setCharacteristicNotification fail"))
            return
        }
        if (mBluetoothGatt?.writeDescriptor(cccd) != true) {
            onError?.invoke(BleException("writeDescriptor fail"))
            return
        }
        onSuccess?.invoke()
    }

    override fun onWriteCharacteristic(
        data: ByteArray,
        characteristicUuid: UUID,
        serviceUuid: UUID?,
        writeType: Int,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        if (data.isEmpty()) {
            onError?.invoke(BleException("data is empty"))
            return
        }
        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
        if (characteristic == null) {
            onError?.invoke(BleException("特征值不存在：${characteristicUuid.getValidString()}"))
            return
        }

        if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            onError?.invoke(BleException("this characteristic not support write!"))
            return
        }

        mConnectCallbackManager.setWriteCharacteristicBleCallback(object : BleCallback<Unit>() {
            override fun onSuccess(data: Unit) {
                onSuccess?.invoke()
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
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
            onError?.invoke(BleException("写特征值失败：${characteristicUuid.getValidString()}"))
        }
    }

    override fun onWriteDescriptor(
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID?,
        serviceUuid: UUID?,
        onSuccess: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        if (data.isEmpty()) {
            onError?.invoke(BleException("data is empty"))
            return
        }
        val descriptor = mBluetoothGatt?.findDescriptor(descriptorUuid, characteristicUuid, serviceUuid)
        if (descriptor == null) {
            onError?.invoke(BleException("描述不存在：${descriptorUuid.getValidString()}"))
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
                onSuccess?.invoke()
            }

            override fun onError(exception: BleException) {
                onError?.invoke(exception)
            }
        })

        descriptor.value = data
        if (mBluetoothGatt?.writeDescriptor(descriptor) != true) {
            onError?.invoke(BleException("写描述值失败：${descriptorUuid.getValidString()}"))
        }
    }

    override fun onSetNotifyCallback(onResult: (ByteArray) -> Unit) {
        mConnectCallbackManager.setReadNotifyBleCallback(object : BleCallback<ByteArray>() {
            override fun onSuccess(data: ByteArray) {
                onResult(data)
            }
        })
    }

    override fun onRemoveNotifyCallback() {
        mConnectCallbackManager.setReadNotifyBleCallback(null)
    }

    override fun getDevice(): BluetoothDevice? {
        return mBluetoothGatt?.device
    }

}