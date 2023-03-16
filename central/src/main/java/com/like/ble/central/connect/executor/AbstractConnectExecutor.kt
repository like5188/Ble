package com.like.ble.central.connect.executor

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.IntRange
import com.like.ble.executor.BaseExecutor
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 中心设备蓝牙命令执行者。
 */
abstract class AbstractConnectExecutor(activity: ComponentActivity) : BaseExecutor(
    activity,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12 中的新蓝牙权限
        // https://developer.android.google.cn/about/versions/12/features/bluetooth-permissions?hl=zh-cn
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
) {
    abstract val notifyFlow: Flow<ByteArray?>

    /**
     * 连接蓝牙设备
     *
     * @return 服务列表
     */
    abstract suspend fun connect(timeout: Long = 10000L): List<BluetoothGattService>?

    /**
     * 断开蓝牙设备
     */
    abstract fun disconnect()

    /**
     * 读特征值，一次最多可以读取600字节
     *
     * @param characteristicUuid        特征UUID
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @return 特征值
     */
    abstract suspend fun readCharacteristic(
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
    abstract suspend fun readDescriptor(
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
    abstract suspend fun readNotify(characteristicUuid: UUID, serviceUuid: UUID? = null)

    abstract suspend fun readRemoteRssi(timeout: Long = 3000L): Int

    /**
     * 快速传输大量数据时设置[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]，完成后要设置成默认的: [android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]
     *
     * @param connectionPriority    需要设置的priority。[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_HIGH]、[android.bluetooth.BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]
     */
    abstract suspend fun requestConnectionPriority(@IntRange(from = 0, to = 2) connectionPriority: Int): Boolean

    abstract suspend fun requestMtu(@IntRange(from = 23, to = 517) mtu: Int, timeout: Long = 3000L): Int

    /**
     * 设置特征的notification或者indication
     *
     * @param characteristicUuid            特征UUID
     * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @param type                          类型：0 (notification 不需要应答)；1 (indication 需要客户端应答)
     * @param enable                        true：开启；false：关闭
     */
    abstract suspend fun setCharacteristicNotification(
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
    abstract suspend fun writeCharacteristic(
        data: List<ByteArray>,
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        timeout: Long = 3000L,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
    )

    /**
     * 写描述值
     *
     * @param data                      需要写入的数据，已经分好包了的，每次传递一个 ByteArray。BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
     * @param descriptorUuid            描述UUID，属于[characteristicUuid]
     * @param characteristicUuid        特征UUID，如果不为null，则会在此特征下查找[descriptorUuid]；如果为null，则会遍历所有特征查找第一个匹配的[descriptorUuid]
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     */
    abstract suspend fun writeDescriptor(
        data: List<ByteArray>,
        descriptorUuid: UUID,
        characteristicUuid: UUID? = null,
        serviceUuid: UUID? = null,
        timeout: Long = 3000L,
    )

}
