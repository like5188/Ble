package com.like.ble.central.connect.callback

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.like.ble.callback.BleCallback
import com.like.ble.exception.BleExceptionDeviceDisconnected
import com.like.ble.exception.BleExceptionDiscoverServices
import com.like.ble.executor.BleExecutor
import com.like.ble.util.getValidString
import com.like.ble.util.isBluetoothEnable
import com.like.ble.util.refreshDeviceCache
import java.util.*

@SuppressLint("MissingPermission")
class ConnectCallbackManager(private val context: Context) {
    // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        // 当连接状态改变
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i("ConnectCallbackManager", "onConnectionStateChange status=$status newState=$newState")
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // 连接蓝牙设备成功
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                gatt.disconnect()
                gatt.refreshDeviceCache()
                gatt.close()
                val e = BleExceptionDeviceDisconnected(gatt.device.address)
                /**
                 * 当断开原因为关闭蓝牙开关时，不回调，由 [BleExecutor.setOnBleEnableListener] 设置的监听来回调。
                 */
                if (context.isBluetoothEnable()) {
                    onDisconnectedListener?.invoke(e)
                }
                connectBleCallback?.onError(e)
            }
        }

        // 发现蓝牙服务
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                connectBleCallback?.onSuccess(gatt.services)
            } else {
                gatt.disconnect()
                gatt.refreshDeviceCache()
                gatt.close()
                val e = BleExceptionDiscoverServices(gatt.device.address)
                if (context.isBluetoothEnable()) {
                    onDisconnectedListener?.invoke(e)
                }
                connectBleCallback?.onError(e)
            }
        }

        // 谁进行读数据操作，然后外围设备才会被动的发出一个数据，而这个数据只能是读操作的对象才有资格获得到这个数据。
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCharacteristicBleCallback?.onSuccess(characteristic.value)
            } else {
                readCharacteristicBleCallback?.onError("读取特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readDescriptorBleCallback?.onSuccess(descriptor.value)
            } else {
                readDescriptorBleCallback?.onError("读取描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

        // 为某个特征启用通知后，如果远程设备上的特征发生更改，则会触发
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            readNotifyBleCallbacks[characteristic.uuid]?.onSuccess(characteristic.value)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                requestMtuBleCallback?.onSuccess(mtu)
            } else {
                requestMtuBleCallback?.onError("failed to set mtu")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readRemoteRssiBleCallback?.onSuccess(rssi)
            } else {
                readRemoteRssiBleCallback?.onError("failed to read remote rssi")
            }
        }

        // 写特征值，注意，这里的characteristic.value中的数据是你写入的数据，而不是外围设备sendResponse返回的。
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeCharacteristicBleCallback?.onSuccess(Unit)
            } else {
                writeCharacteristicBleCallback?.onError("写特征值失败：${characteristic.uuid.getValidString()}")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeDescriptorBleCallback?.onSuccess(Unit)
            } else {
                writeDescriptorBleCallback?.onError("写描述值失败：${descriptor.uuid.getValidString()}")
            }
        }

    }

    private var connectBleCallback: BleCallback<List<BluetoothGattService>>? = null
    private var readCharacteristicBleCallback: BleCallback<ByteArray>? = null
    private var readDescriptorBleCallback: BleCallback<ByteArray>? = null
    private var readRemoteRssiBleCallback: BleCallback<Int>? = null
    private var requestMtuBleCallback: BleCallback<Int>? = null
    private var writeCharacteristicBleCallback: BleCallback<Unit>? = null
    private var writeDescriptorBleCallback: BleCallback<Unit>? = null
    private var readNotifyBleCallbacks = mutableMapOf<UUID, BleCallback<ByteArray>>()
    private var onDisconnectedListener: ((Throwable) -> Unit)? = null

    fun getBluetoothGattCallback(): BluetoothGattCallback {
        return bluetoothGattCallback
    }

    fun setConnectBleCallback(callback: BleCallback<List<BluetoothGattService>>?) {
        connectBleCallback = callback
    }

    fun setReadCharacteristicBleCallback(callback: BleCallback<ByteArray>?) {
        readCharacteristicBleCallback = callback
    }

    fun setReadDescriptorBleCallback(callback: BleCallback<ByteArray>?) {
        readDescriptorBleCallback = callback
    }

    fun setReadRemoteRssiBleCallback(callback: BleCallback<Int>?) {
        readRemoteRssiBleCallback = callback
    }

    fun setRequestMtuBleCallback(callback: BleCallback<Int>?) {
        requestMtuBleCallback = callback
    }

    fun setWriteCharacteristicBleCallback(callback: BleCallback<Unit>?) {
        writeCharacteristicBleCallback = callback
    }

    fun setWriteDescriptorBleCallback(callback: BleCallback<Unit>?) {
        writeDescriptorBleCallback = callback
    }

    fun setReadNotifyBleCallback(characteristicUuid: UUID, callback: BleCallback<ByteArray>?) {
        if (callback == null) {
            readNotifyBleCallbacks.remove(characteristicUuid)
        } else {
            readNotifyBleCallbacks[characteristicUuid] = callback
        }
    }

    fun setOnDisconnectedListener(listener: ((Throwable) -> Unit)? = null) {
        onDisconnectedListener = listener
    }

}
