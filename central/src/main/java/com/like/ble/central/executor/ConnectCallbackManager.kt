package com.like.ble.central.executor

import android.annotation.SuppressLint
import android.bluetooth.*
import com.like.ble.exception.BleException
import com.like.ble.util.getValidString

@SuppressLint("MissingPermission")
class ConnectCallbackManager {
    internal var connectCallback: ConnectCallback? = null
    internal var readCharacteristicCallback: ByteArrayCallback? = null
    internal var readDescriptorCallback: ByteArrayCallback? = null
    internal var readNotifyCallback: ByteArrayCallback? = null

    // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
    internal val gattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // 连接蓝牙设备成功
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                connectCallback?.onError("连接蓝牙失败：${gatt.device.address}")
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                connectCallback?.onSuccess(gatt.services)
            } else {
                connectCallback?.onError("发现服务失败：${gatt.device.address}")
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCharacteristicCallback?.onSuccess(characteristic.value)
            } else {
                readCharacteristicCallback?.onError("读取特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readDescriptorCallback?.onSuccess(descriptor.value)
            } else {
                readDescriptorCallback?.onError("读取描述失败：${descriptor.uuid.getValidString()}")
            }
        }

        // 为某个特征启用通知后，如果远程设备上的特征发生更改，则会触发
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            readNotifyCallback?.onSuccess(characteristic.value)
        }

    }

}

abstract class BleCallback {
    fun onError(msg: String) {
        onError(BleException(msg))
    }

    open fun onError(exception: BleException) {}
}

abstract class ConnectCallback : BleCallback() {
    abstract fun onSuccess(services: List<BluetoothGattService>?)
}

abstract class ByteArrayCallback : BleCallback() {
    abstract fun onSuccess(data: ByteArray?)
}
