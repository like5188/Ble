package com.like.ble.central.callback

import android.annotation.SuppressLint
import android.bluetooth.*
import com.like.ble.exception.BleException
import com.like.ble.util.getValidString

@SuppressLint("MissingPermission")
class ConnectCallbackManager {
    // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
    private val gattCallback = object : BluetoothGattCallback() {
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

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                requestMtuCallback?.onSuccess(mtu)
            } else {
                requestMtuCallback?.onError("failed to set mtu")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readRemoteRssiCallback?.onSuccess(rssi)
            } else {
                readRemoteRssiCallback?.onError("failed to read remote rssi")
            }
        }

        // 写特征值，注意，这里的characteristic.value中的数据是你写入的数据，而不是外围设备sendResponse返回的。
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeCharacteristicCallback?.onSuccess()
            } else {
                writeCharacteristicCallback?.onError("写特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeDescriptorCallback?.onSuccess()
            } else {
                writeDescriptorCallback?.onError("写描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

    }

    private var connectCallback: ConnectCallback? = null
    private var readCharacteristicCallback: ByteArrayCallback? = null
    private var readDescriptorCallback: ByteArrayCallback? = null
    private var readNotifyCallback: ByteArrayCallback? = null
    private var readRemoteRssiCallback: IntCallback? = null
    private var requestMtuCallback: IntCallback? = null
    private var writeCharacteristicCallback: BleCallback? = null
    private var writeDescriptorCallback: BleCallback? = null

    fun getBluetoothGattCallback(): BluetoothGattCallback {
        return gattCallback
    }

    fun setConnectCallback(callback: ConnectCallback?) {
        connectCallback = callback
    }

    fun setReadCharacteristicCallback(callback: ByteArrayCallback?) {
        readCharacteristicCallback = callback
    }

    fun setReadDescriptorCallback(callback: ByteArrayCallback?) {
        readDescriptorCallback = callback
    }

    fun setReadNotifyCallback(callback: ByteArrayCallback?) {
        readNotifyCallback = callback
    }

    fun setReadRemoteRssiCallback(callback: IntCallback?) {
        readRemoteRssiCallback = callback
    }

    fun setRequestMtuCallback(callback: IntCallback?) {
        requestMtuCallback = callback
    }

    fun setWriteCharacteristicCallback(callback: BleCallback?) {
        writeCharacteristicCallback = callback
    }

    fun setWriteDescriptorCallback(callback: BleCallback?) {
        writeDescriptorCallback = callback
    }

}

abstract class BleCallback {
    open fun onSuccess() {}

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

abstract class IntCallback : BleCallback() {
    abstract fun onSuccess(data: Int)
}
