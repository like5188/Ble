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

    override suspend fun connect(address: String, timeout: Long): List<BluetoothGattService>? {
        if (!activity.isBluetoothEnableAndSettingIfDisabled()) {
            throw BleException("蓝牙未打开")
        }
        if (!PermissionUtils.requestPermissions(activity, false)) {
            throw BleException("蓝牙权限被拒绝")
        }
        if (isConnected()) return mBluetoothGatt?.services
        // 获取远端的蓝牙设备
        val bluetoothDevice = activity.getBluetoothAdapter()?.getRemoteDevice(address) ?: throw BleException("连接蓝牙失败：$address 未找到")
        return suspendCancellableCoroutineWithTimeout(timeout) { continuation ->
            // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
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
        if (isConnected()) {
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
        if (!isConnected()) {
            throw BleException("蓝牙未连接：$address")
        }

        val characteristic = mBluetoothGatt?.findCharacteristic(characteristicUuid, serviceUuid)
            ?: throw BleException("特征值不存在：${characteristicUuid.getValidString()}")

        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            throw BleException("this characteristic not support read!")
        }

        return suspendCancellableCoroutineWithTimeout(timeout) { continuation ->
            // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
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

    private fun isConnected(): Boolean {
        val device = mBluetoothGatt?.device ?: return false
        return activity.isBleDeviceConnected(device)
    }

    override suspend fun close() {
        disconnect()
    }
}