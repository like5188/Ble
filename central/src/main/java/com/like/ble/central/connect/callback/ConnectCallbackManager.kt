package com.like.ble.central.connect.callback

import android.annotation.SuppressLint
import android.bluetooth.*
import android.util.Log
import com.like.ble.callback.BleCallback
import com.like.ble.exception.BleExceptionDeviceDisconnected
import com.like.ble.exception.BleExceptionDiscoverServices
import com.like.ble.util.getValidString
import com.like.ble.util.refreshDeviceCache

@SuppressLint("MissingPermission")
class ConnectCallbackManager {
    // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
    private val mBluetoothGattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i("TAG", "onConnectionStateChange status=$status newState=$newState")
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // 连接蓝牙设备成功
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                gatt.disconnect()
                gatt.refreshDeviceCache()
                gatt.close()
                connectCallback?.onError(BleExceptionDeviceDisconnected(gatt.device.address))
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                connectCallback?.onSuccess(gatt.services)
            } else {
                gatt.disconnect()
                gatt.refreshDeviceCache()
                gatt.close()
                connectCallback?.onError(BleExceptionDiscoverServices(gatt.device.address))
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
                readDescriptorCallback?.onError("读取描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

        // 为某个特征启用通知后，如果远程设备上的特征发生更改，则会触发
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            readNotifyCallback?.onSuccess(characteristic)
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
                writeCharacteristicCallback?.onSuccess(Unit)
            } else {
                writeCharacteristicCallback?.onError("写特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeDescriptorCallback?.onSuccess(Unit)
            } else {
                writeDescriptorCallback?.onError("写描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

    }

    private var connectCallback: BleCallback<List<BluetoothGattService>>? = null
    private var readCharacteristicCallback: BleCallback<ByteArray>? = null
    private var readDescriptorCallback: BleCallback<ByteArray>? = null
    private var readRemoteRssiCallback: BleCallback<Int>? = null
    private var requestMtuCallback: BleCallback<Int>? = null
    private var writeCharacteristicCallback: BleCallback<Unit>? = null
    private var writeDescriptorCallback: BleCallback<Unit>? = null
    private var readNotifyCallback: BleCallback<BluetoothGattCharacteristic>? = null

    fun getBluetoothGattCallback(): BluetoothGattCallback {
        return mBluetoothGattCallback
    }

    fun setConnectCallback(callback: BleCallback<List<BluetoothGattService>>?) {
        connectCallback = callback
    }

    fun setReadCharacteristicCallback(callback: BleCallback<ByteArray>?) {
        readCharacteristicCallback = callback
    }

    fun setReadDescriptorCallback(callback: BleCallback<ByteArray>?) {
        readDescriptorCallback = callback
    }

    fun setReadRemoteRssiCallback(callback: BleCallback<Int>?) {
        readRemoteRssiCallback = callback
    }

    fun setRequestMtuCallback(callback: BleCallback<Int>?) {
        requestMtuCallback = callback
    }

    fun setWriteCharacteristicCallback(callback: BleCallback<Unit>?) {
        writeCharacteristicCallback = callback
    }

    fun setWriteDescriptorCallback(callback: BleCallback<Unit>?) {
        writeDescriptorCallback = callback
    }

    fun setReadNotifyCallback(callback: BleCallback<BluetoothGattCharacteristic>?) {
        readNotifyCallback = callback
    }

}
