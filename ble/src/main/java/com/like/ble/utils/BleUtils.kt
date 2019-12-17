package com.like.ble.utils

import android.bluetooth.*
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
internal fun BluetoothGatt.findCharacteristic(characteristicUuidString: String): BluetoothGattCharacteristic? {
    val characteristicUuid = UUID.fromString(characteristicUuidString)
    var characteristic: BluetoothGattCharacteristic?
    services.forEach {
        characteristic = it.getCharacteristic(characteristicUuid)
        if (characteristic != null) {
            return characteristic
        }
    }
    return null
}

fun getConnectionStateString(status: Int) = when (status) {
    0 -> "DISCONNECTED"
    1 -> "CONNECTING"
    2 -> "CONNECTED"
    3 -> "DISCONNECTING"
    else -> ""
}

fun getBluetoothGattStatusString(status: Int) = when (status) {
    0 -> "SUCCESS"
    0x2 -> "READ_NOT_PERMITTED"
    0x3 -> "WRITE_NOT_PERMITTED"
    0x5 -> "INSUFFICIENT_AUTHENTICATION"
    0x6 -> "REQUEST_NOT_SUPPORTED"
    0xf -> "INSUFFICIENT_ENCRYPTION"
    0x7 -> "INVALID_OFFSET"
    0xd -> "INVALID_ATTRIBUTE_LENGTH"
    0x8f -> "CONNECTION_CONGESTED"
    0x101 -> "FAILURE"
    else -> ""
}

fun getUuidValidString(uuid: String) = "0x${uuid.substring(4, 8)}"

fun UUID.getValidString(): String = "0x${toString().substring(4, 8)}"

fun BluetoothGattService.getTypeString() = when (type) {
    0 -> "PRIMARY"
    1 -> "SECONDARY"
    else -> ""
}

fun BluetoothGattCharacteristic.getPropertiesString(): String {
    val result = StringBuilder()
    if (properties and 0x01 != 0) {
        result.append("BROADCAST；")
    }
    if (properties and 0x02 != 0) {
        result.append("READ；")
    }
    if (properties and 0x04 != 0) {
        result.append("WRITE_NO_RESPONSE；")
    }
    if (properties and 0x08 != 0) {
        result.append("WRITE；")
    }
    if (properties and 0x10 != 0) {
        result.append("NOTIFY；")
    }
    if (properties and 0x20 != 0) {
        result.append("INDICATE；")
    }
    if (properties and 0x40 != 0) {
        result.append("SIGNED_WRITE；")
    }
    if (properties and 0x80 != 0) {
        result.append("EXTENDED_PROPS；")
    }
    return if (result.isEmpty()) {
        ""
    } else {
        result.substring(0, result.lastIndex)
    }
}

/**
 * 把 ByteArray 按照指定的 chunkSize 进行分批处理
 */
fun ByteArray.batch(chunkSize: Int): List<ByteArray> {
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