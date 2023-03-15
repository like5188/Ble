package com.like.ble.central.executor

import android.bluetooth.BluetoothGattService
import com.like.ble.executor.IExecutor
import java.util.*

/**
 * 中心设备蓝牙命令执行者。
 */
interface IConnectExecutor : IExecutor {

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

}
