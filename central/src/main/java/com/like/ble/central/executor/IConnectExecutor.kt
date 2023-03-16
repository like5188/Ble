package com.like.ble.central.executor

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.annotation.IntRange
import com.like.ble.executor.IExecutor
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 中心设备蓝牙命令执行者。
 */
interface IConnectExecutor : IExecutor {
    val notifyFlow: Flow<ByteArray?>

    /**
     * 连接蓝牙设备
     *
     * @return 服务列表
     */
    suspend fun connect(
        address: String,
        timeout: Long = 10000L,
    ): List<BluetoothGattService>?

    /**
     * 断开蓝牙设备
     */
    fun disconnect()

    /**
     * 读特征值，一次最多可以读取600字节
     *
     * @param characteristicUuid        特征UUID
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @return 特征值
     */
    suspend fun readCharacteristic(
        address: String,
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        timeout: Long = 3000L,
    ): ByteArray?

    /**
     * 读描述值，一次最多可以读取600字节
     *
     * @param descriptorUuid            描述UUID，属于[characteristicUuid]
     * @param characteristicUuid        特征UUID，如果不为null，则会在此特征下查找[descriptorUuid]；如果为null，则会遍历所有特征查找第一个匹配的[descriptorUuid]
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     */
    suspend fun readDescriptor(
        address: String,
        descriptorUuid: UUID,
        characteristicUuid: UUID? = null,
        serviceUuid: UUID? = null,
        timeout: Long = 3000L,
    ): ByteArray?

    /**
     * 读取通知传来的数据（通过notify或者indicate的方式），数据从[notifyFlow]获取
     * 使用后相当于设置了一个回调监听，再配合[writeCharacteristic]发送命令并接收通知数据，注意必须要开启通知才能接收数据。这样比单独使用[readCharacteristic]命令来读取数据快很多。
     *
     * @param characteristicUuid        特征UUID
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     */
    suspend fun readNotify(
        address: String,
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
    )

    suspend fun readRemoteRssi(
        address: String,
        timeout: Long = 3000L,
    ): Int

    /**
     * 快速传输大量数据时设置[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]，完成后要设置成默认的: [android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]
     *
     * @param connectionPriority    需要设置的priority。[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]
     */
    suspend fun requestConnectionPriority(
        address: String,
        @IntRange(from = 0, to = 2)
        connectionPriority: Int,
    ): Boolean

    suspend fun requestMtu(
        address: String,
        @IntRange(from = 23, to = 517)
        mtu: Int,
        timeout: Long = 3000L,
    ): Int

    /**
     * 设置特征的notification或者indication
     *
     * @param characteristicUuid            特征UUID
     * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @param type                          类型：0 (notification 不需要应答)；1 (indication 需要客户端应答)
     * @param enable                        true：开启；false：关闭
     */
    suspend fun setCharacteristicNotification(
        address: String,
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        @IntRange(from = 0, to = 1)
        type: Int = 0,
        enable: Boolean = true,
    ): Boolean

    /**
     * 写特征值
     *
     * @param data                      需要写入的数据，已经分好包了的，每次传递一个 ByteArray。BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
     * @param characteristicUuid        特征UUID
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @param writeType
     * WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
     * WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
     * WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
     */
    suspend fun writeCharacteristic(
        address: String,
        data: List<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        timeout: Long = 3000L,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
    )

}
