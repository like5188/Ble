package com.like.ble.util

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import java.nio.ByteBuffer
import java.util.*
import java.util.regex.Pattern
import kotlin.math.ceil

@SuppressLint("MissingPermission")
fun Context.isBleDeviceConnected(device: BluetoothDevice?): Boolean =
    if (device == null) {
        false
    } else {
        getBluetoothManager()?.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
    }

@SuppressLint("MissingPermission")
fun Context.isBleDeviceConnected(address: String?): Boolean =
    if (address.isNullOrEmpty()) {
        false
    } else {
        getBluetoothManager()?.getConnectedDevices(BluetoothProfile.GATT)?.any { it.address == address } ?: false
    }

/**
 * Context生命周期内不会改变
 */
fun Context.getBluetoothManager(): BluetoothManager? = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

/**
 * app生命周期内不会改变
 */
fun Context.getBluetoothAdapter(): BluetoothAdapter? = getBluetoothManager()?.adapter

/**
 * 蓝牙是否准备就绪
 */
fun Context.isBluetoothEnable(): Boolean = getBluetoothAdapter()?.isEnabled ?: false

/**
 * 查看设备是否支持蓝牙功能
 */
fun Context.isSupportBluetooth(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

/**
 * 蓝牙是否打开。
 * 如果没打开，就去打开
 * 打开蓝牙
 */
suspend fun ComponentActivity.isBluetoothEnableAndSettingIfDisabled(): Boolean = if (isBluetoothEnable()) {
    true
} else {// 蓝牙功能未打开
    // 弹出开启蓝牙的对话框
    startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)).resultCode == Activity.RESULT_OK
}

/**
 * 清除内部缓存，并强制从远程设备刷新服务
 */
fun BluetoothGatt.refreshDeviceCache(): Boolean = try {
    (BluetoothGatt::class.java.getMethod("refresh").invoke(this) as? Boolean) ?: false
} catch (e: Exception) {
    e.printStackTrace()
    false
}

/**
 * 查找远程设备的特征
 */
fun BluetoothGatt.findCharacteristic(characteristicUuid: UUID, serviceUuid: UUID? = null): BluetoothGattCharacteristic? {
    if (serviceUuid != null) {
        return getService(serviceUuid)?.getCharacteristic(characteristicUuid)
    } else {
        services.forEach {
            val characteristic = it.getCharacteristic(characteristicUuid)
            if (characteristic != null) {
                return characteristic
            }
        }
        return null
    }
}

/**
 * 查找远程设备的描述
 */
fun BluetoothGatt.findDescriptor(
    descriptorUuid: UUID,
    characteristicUuid: UUID? = null,
    serviceUuid: UUID? = null
): BluetoothGattDescriptor? {
    when {
        serviceUuid != null && characteristicUuid != null -> {
            return services.firstOrNull { it.uuid == serviceUuid }?.getCharacteristic(characteristicUuid)?.getDescriptor(descriptorUuid)
        }

        serviceUuid != null -> {
            services.firstOrNull { it.uuid == serviceUuid }?.characteristics?.forEach {
                val descriptor = it.getDescriptor(descriptorUuid)
                if (descriptor != null) {
                    return descriptor
                }
            }
            return null
        }

        characteristicUuid != null -> {
            services.forEach {
                val characteristic = it.getCharacteristic(characteristicUuid)
                if (characteristic != null) {
                    val descriptor = characteristic.getDescriptor(descriptorUuid)
                    if (descriptor != null) {
                        return descriptor
                    }
                }
            }
            return null
        }

        else -> {
            services.forEach { service ->
                service.characteristics.forEach { characteristic ->
                    val descriptor = characteristic.getDescriptor(descriptorUuid)
                    if (descriptor != null) {
                        return descriptor
                    }
                }
            }
            return null
        }
    }
}

fun getConnectionStateString(status: Int) = when (status) {
    BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
    BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
    BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
    BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
    else -> ""
}

fun getBluetoothGattStatusString(status: Int) = when (status) {
    BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
    BluetoothGatt.GATT_READ_NOT_PERMITTED -> "READ_NOT_PERMITTED"
    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "WRITE_NOT_PERMITTED"
    BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "INSUFFICIENT_AUTHENTICATION"
    BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "REQUEST_NOT_SUPPORTED"
    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "INSUFFICIENT_ENCRYPTION"
    BluetoothGatt.GATT_INVALID_OFFSET -> "INVALID_OFFSET"
    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "INVALID_ATTRIBUTE_LENGTH"
    BluetoothGatt.GATT_CONNECTION_CONGESTED -> "CONNECTION_CONGESTED"
    BluetoothGatt.GATT_FAILURE -> "FAILURE"
    else -> ""
}

fun BluetoothGattService.getTypeString() = when (type) {
    BluetoothGattService.SERVICE_TYPE_PRIMARY -> "PRIMARY"
    BluetoothGattService.SERVICE_TYPE_SECONDARY -> "SECONDARY"
    else -> "UNKNOWN TYPE"
}

fun BluetoothGattCharacteristic.getPropertiesString(): String {
    val result = StringBuilder()
    if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) {
        result.append("BROADCAST；")
    }
    if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
        result.append("READ；")
    }
    if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
        result.append("WRITE_NO_RESPONSE；")
    }
    if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
        result.append("WRITE；")
    }
    if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
        result.append("NOTIFY；")
    }
    if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
        result.append("INDICATE；")
    }
    if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) {
        result.append("SIGNED_WRITE；")
    }
    if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) {
        result.append("EXTENDED_PROPS；")
    }
    if (result.isNotEmpty()) {
        result.deleteLast()
    }
    return result.toString()
}

fun createBleUuidBy16Bit(uuidString: String?): UUID {
    if (uuidString?.length != 4) {
        throw IllegalArgumentException("uuidString 的长度必须为 4")
    }
    return createBleUuidBy32Bit("0000$uuidString")
}

fun createBleUuidBy32Bit(uuidString: String?): UUID {
    if (uuidString?.length != 8) {
        throw IllegalArgumentException("uuidString 的长度必须为 8")
    }
    return "$uuidString-0000-1000-8000-00805F9B34FB".toUUID()
}

fun createBleUuidOrNullBy16Bit(uuidString: String?): UUID? {
    if (uuidString?.length != 4) {
        return null
    }
    return createBleUuidOrNullBy32Bit("0000$uuidString")
}

fun createBleUuidOrNullBy32Bit(uuidString: String?): UUID? {
    if (uuidString?.length != 8) {
        return null
    }
    return "$uuidString-0000-1000-8000-00805F9B34FB".toUUIDOrNull()
}

fun String?.toUUID(): UUID {
    if (this.isNullOrEmpty()) {
        throw IllegalArgumentException("toUUID failure, string is null or empty")
    }
    return UUID.fromString(this)
}

fun String?.toUUIDOrNull(): UUID? {
    if (this.isNullOrEmpty()) {
        return null
    }
    return try {
        UUID.fromString(this)
    } catch (e: Exception) {
        null
    }
}

fun StringBuilder.deleteLast() {
    deleteCharAt(length - 1)
}

fun Int.toHexString2(): String {
    return String.format("%02x", this).uppercase()
}

fun Int.toHexString4(): String {
    return String.format("%04x", this).uppercase()
}

fun ByteArray.toHexString(): String {
    val sb = StringBuilder()
    for (b in this) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString().uppercase()
}

/**
 * 16进制表示的字符串转换为字节数组
 *
 * @return byte[] 字节数组
 */
fun String?.hexStringToByteArray(): ByteArray {
    if (this == null || this.trim().isEmpty()) {
        return byteArrayOf()
    }
    val hexString = this.replace(" ", "")
    val len = hexString.length
    val bytes = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个字节
        bytes[i / 2] = ((Character.digit(hexString[i], 16) shl 4) + Character.digit(hexString[i + 1], 16)).toByte()
        i += 2
    }
    return bytes
}

fun UUID.getValidString(): String = "0x${toString().substring(4, 8).uppercase()}"

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
 * 复制一部分数据。如果数据不够，返回的数组长度会小于length
 *
 * @param fromIndex     开始位置索引
 * @param length        截取的长度
 */
fun ByteArray.copyOfRangeByLength(fromIndex: Int, length: Int): ByteArray {
    val responseSize = if (size - fromIndex > length) {
        length
    } else {
        size - fromIndex
    }
    val response = ByteArray(responseSize)
    for (i in fromIndex until fromIndex + responseSize) {
        response[i - fromIndex] = this[i]
    }
    return response
}

/**
 * ByteBuffer 转换成 ByteArray
 */
fun ByteBuffer.toByteArrayOrNull(): ByteArray? {
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

fun String.isHexString() = Pattern.matches("^[A-Fa-f0-9]+\$", this)