package com.like.ble.utils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.ceil

internal fun Context.getBluetoothManager() = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

/**
 * 查找远程设备的特征，并开启通知，以便触发onCharacteristicChanged()方法
 */
internal fun BluetoothGatt.findCharacteristic(
    characteristicUuidString: String,
    enableNotification: Boolean = false
): BluetoothGattCharacteristic? {
    // 开始查找特征
    val characteristic = services
        ?.flatMap {
            it.characteristics
        }
        ?.firstOrNull {
            it.uuid.toString() == characteristicUuidString
        }

    if (enableNotification && characteristic != null) {
        // 接受Characteristic被写的通知,收到蓝牙模块的数据后会触发onCharacteristicChanged()
        setCharacteristicNotification(characteristic, true)
    }
    return characteristic
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