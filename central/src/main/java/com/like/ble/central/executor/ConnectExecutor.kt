package com.like.ble.central.executor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattService
import android.os.Build
import androidx.activity.ComponentActivity
import com.like.ble.exception.BleException
import com.like.ble.util.getBluetoothAdapter
import com.like.ble.util.isBleDeviceConnected
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 蓝牙连接及连接成功后的命令执行者
 * 可以进行连接、断开连接、操作数据等等操作
 */
@SuppressLint("MissingPermission")
class ConnectExecutor(private val mActivity: ComponentActivity) : IConnectExecutor {
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mGattCallback: BluetoothGattCallback? = null

    override suspend fun connect(address: String, timeout: Long): List<BluetoothGattService>? {
        if (isConnected()) return mBluetoothGatt?.services
        // 获取远端的蓝牙设备
        val bluetoothDevice = mActivity.getBluetoothAdapter()?.getRemoteDevice(address) ?: throw BleException("连接蓝牙失败：$address 未找到")

        suspend fun con(): List<BluetoothGattService>? = suspendCoroutine { continuation ->
            // 蓝牙Gatt回调方法中都不可以进行耗时操作，需要将其方法内进行的操作丢进另一个线程，尽快返回。
            val callback = object : BluetoothGattCallback() {
                // 当连接状态改变
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        // 连接蓝牙设备成功
                        gatt.discoverServices()
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        disconnectAndCloseBluetoothGatt()
                        continuation.resumeWithException(BleException("连接蓝牙失败：${gatt.device.address}"))
                    }
                }

                // 发现蓝牙服务
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {// 发现了蓝牙服务后，才算真正的连接成功。
                        continuation.resume(gatt.services)
                    } else {
                        disconnectAndCloseBluetoothGatt()
                        continuation.resumeWithException(BleException("发现服务失败：${gatt.device.address}"))
                    }
                }
            }
            mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothDevice.connectGatt(
                    mActivity,
                    false,
                    callback,
                    BluetoothDevice.TRANSPORT_LE
                )// 第二个参数表示是否自动重连
            } else {
                bluetoothDevice.connectGatt(mActivity, false, callback)// 第二个参数表示是否自动重连
            }
            mGattCallback = callback
        }

        return if (timeout > 0) {
            withTimeout(timeout) {
                con()
            }
        } else {
            con()
        }
    }

    override suspend fun disconnect() {
        disconnectAndCloseBluetoothGatt()
    }

    private fun disconnectAndCloseBluetoothGatt() {
        if (isConnected()) {
            mBluetoothGatt?.disconnect()
        }
        // 这里的close()方法会清空BluetoothGatt中的mGattCallback，导致收不到回调，但是可以使用本地缓存的各个命令来回调
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    private fun isConnected(): Boolean {
        val device = mBluetoothGatt?.device ?: return false
        return mActivity.isBleDeviceConnected(device)
    }

    override suspend fun close() {
        disconnectAndCloseBluetoothGatt()
    }
}