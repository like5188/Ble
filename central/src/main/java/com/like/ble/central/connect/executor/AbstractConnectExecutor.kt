package com.like.ble.central.connect.executor

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.os.Build
import androidx.annotation.IntRange
import com.like.ble.executor.BleExecutor
import com.like.ble.util.isBleDeviceConnected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 中心设备蓝牙连接及数据操作的执行者。
 */
abstract class AbstractConnectExecutor(context: Context, val address: String?) : BleExecutor(context) {

    final override fun getPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 中的新蓝牙权限
            // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun isBleDeviceConnected(): Boolean = mContext.isBleDeviceConnected(address)

    abstract fun getDevice(): BluetoothDevice?

    /**
     * 连接蓝牙设备
     *
     * @param onDisconnectedListener    如果连接成功后再断开，就会触发此回调。
     * 注意：当断开原因为关闭蓝牙开关时，不回调，由 [BleExecutor.setOnBleEnableListener] 设置的监听来回调。
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun connect(
        timeout: Long = 10000L,
        onDisconnectedListener: ((Throwable) -> Unit)? = null
    ): List<BluetoothGattService>

    /**
     * 断开蓝牙设备
     * 注意：断开连接不会触发[onConnectionStateChange]回调
     * @throws [com.like.ble.exception.BleException]
     */
    abstract fun disconnect()

    /**
     * 读特征值，一次最多可以读取600字节，不用组包
     *
     * @param characteristicUuid        特征UUID
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @return 特征值
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun readCharacteristic(
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        timeout: Long = 3000L,
    ): ByteArray

    /**
     * 读描述值，一次最多可以读取600字节
     *
     * @param descriptorUuid            描述UUID，属于[characteristicUuid]
     * @param characteristicUuid        特征UUID，如果不为null，则会在此特征下查找[descriptorUuid]；如果为null，则会遍历所有特征查找第一个匹配的[descriptorUuid]
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     *
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun readDescriptor(
        descriptorUuid: UUID,
        characteristicUuid: UUID? = null,
        serviceUuid: UUID? = null,
        timeout: Long = 3000L,
    ): ByteArray

    /**
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun readRemoteRssi(timeout: Long = 3000L): Int

    /**
     * 快速传输大量数据时设置[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]，完成后要设置成默认的: [android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]
     *
     * @param connectionPriority    需要设置的priority。[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]
     *
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun requestConnectionPriority(@IntRange(from = 0, to = 2) connectionPriority: Int, timeout: Long = 3000L)

    /**
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun requestMtu(@IntRange(from = 23, to = 517) mtu: Int, timeout: Long = 3000L): Int

    /**
     * 设置特征的notification或者indication，开启后，数据从[setNotifyCallback]获取，需要自己组包。
     * 配合[writeCharacteristic]发送命令并接收通知数据，注意必须要开启通知才能接收数据。
     *
     * @param characteristicUuid            特征UUID
     * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @param type                          类型：0 (notification 不需要应答)；1 (indication 需要客户端应答)
     * @param enable                        true：开启；false：关闭
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun setCharacteristicNotification(
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        @IntRange(from = 0, to = 1)
        type: Int = 0,
        enable: Boolean = true,
        timeout: Long = 3000L,
    )

    /**
     * 写特征值并等待通知返回数据
     *
     * @param data                      需要写入的数据。
     * BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU。
     * 如果数据大于20字节，则会超时失败，更会导致蓝牙连接断开
     * @param writeUuid                 写特征UUID
     * @param notifyUuid                通知特征UUID
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @param notifyType                类型：0 (notification 不需要应答)；1 (indication 需要客户端应答)
     * @param writeType
     * WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
     * WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
     * WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
     * @param isWholePackage            是否是一个完整的数据包
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun writeWithResponse(
        data: ByteArray,
        writeUuid: UUID,
        notifyUuid: UUID,
        serviceUuid: UUID? = null,
        timeout: Long = 15000L,
        @IntRange(from = 0, to = 1)
        notifyType: Int = 0,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        isWholePackage: (ByteArray) -> Boolean
    ): ByteArray

    /**
     * 写特征值
     *
     * @param data                      需要写入的数据。
     * BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU。
     * 如果数据大于20字节，则会超时失败，更会导致蓝牙连接断开
     * @param characteristicUuid        特征UUID
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @param writeType
     * WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
     * WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
     * WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun writeCharacteristic(
        data: ByteArray,
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        timeout: Long = 10000L,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
    )

    /**
     * 写描述值
     *
     * @param data                      需要写入的数据。
     * BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU。
     * 如果数据大于20字节，则会超时失败，更会导致蓝牙连接断开
     * @param descriptorUuid            描述UUID，属于[characteristicUuid]
     * @param characteristicUuid        特征UUID，如果不为null，则会在此特征下查找[descriptorUuid]；如果为null，则会遍历所有特征查找第一个匹配的[descriptorUuid]
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     *
     * @throws [com.like.ble.exception.BleException]
     */
    abstract suspend fun writeDescriptor(
        data: ByteArray,
        descriptorUuid: UUID,
        characteristicUuid: UUID? = null,
        serviceUuid: UUID? = null,
        timeout: Long = 10000L,
    )

    /**
     * 设置通知监听
     *
     * 注意：
     * 1、需要配合[setCharacteristicNotification]来使用。
     * 2、可以使用[CoroutineScope.cancel]方法来取消协程作用域，从而会自动移除此监听。
     * 即 flow 停止收集则自动移除此监听，否则会一直存在。
     */
    abstract fun setNotifyCallback(): Flow<ByteArray>

}
