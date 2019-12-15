package com.like.ble.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.ceil

fun Context.getBluetoothManager(): BluetoothManager? = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

fun Context.getBluetoothAdapter(): BluetoothAdapter? = getBluetoothManager()?.adapter

/**
 * 蓝牙是否准备就绪
 */
fun Context.isBluetoothEnable(): Boolean = getBluetoothAdapter()?.isEnabled ?: false

/**
 * 查看手机是否支持蓝牙功能
 */
fun Context.isSupportBluetooth(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

/**
 * 查找远程设备的特征，并开启通知，以便触发onCharacteristicChanged()方法
 */
internal fun BluetoothGatt.findCharacteristic(characteristicUuidString: String): BluetoothGattCharacteristic? =
    services
        ?.flatMap {
            it.characteristics
        }
        ?.firstOrNull {
            it.uuid.toString() == characteristicUuidString
        }

/**
 * 把 ByteArray 按照指定的 chunkSize 进行分批处理
 */
internal fun ByteArray.batch(chunkSize: Int): List<ByteArray> {
    val result = ArrayList<ByteArray>()
    val packetSize = ceil(size / chunkSize.toDouble()).toInt()
    for (i in 0 until packetSize) {
        if (i == packetSize - 1) {
            var lastLen = size % chunkSize
            if (lastLen == 0) {
                lastLen = chunkSize
            }
            val temp = ByteArray(lastLen)
            System.arraycopy(this, i * chunkSize, temp, 0, lastLen)
            result.add(temp)
        } else {
            val temp = ByteArray(chunkSize)
            System.arraycopy(this, i * chunkSize, temp, 0, chunkSize)
            result.add(temp)
        }
    }
    return result
}

/**
 * ByteBuffer 转换成 ByteArray
 */
internal fun ByteBuffer.toByteArrayOrNull(): ByteArray? {
    flip()
    val len = limit() - position()
    val bytes = ByteArray(len)

    if (isReadOnly) {
        return null
    } else {
        get(bytes)
    }
    return bytes
}