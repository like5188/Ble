package com.like.ble.central.executor

import android.bluetooth.BluetoothGattService
import androidx.annotation.IntRange
import com.like.ble.central.command.ReadCharacteristicCommand
import com.like.ble.central.command.WriteCharacteristicCommand
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
     * 使用后相当于设置了一个回调监听，再配合[WriteCharacteristicCommand]发送命令并接收通知数据，注意必须要开启通知才能接收数据。这样比单独使用[ReadCharacteristicCommand]命令来读取数据快很多。
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

}
