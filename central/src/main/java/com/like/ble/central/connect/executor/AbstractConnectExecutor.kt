package com.like.ble.central.connect.executor

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import androidx.annotation.IntRange
import com.like.ble.executor.BleExecutor
import com.like.ble.util.isBleDeviceConnected
import com.like.ble.util.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 中心设备蓝牙连接及数据操作的执行者。
 */
abstract class AbstractConnectExecutor(context: Context, val address: String?) : BleExecutor(context) {

    /**
     * 蓝牙设备是否已经连接
     * 注意：
     * 当我们同时使用connectExecutor中的连接方法和其它方法的时候，不能单独使用 isBleDeviceConnected() 方法来判断是否连接。
     * 因为有可能这个方法判断已经连接，但是由于还没有释放锁，造成你后续操作其它方法失败，
     * 比如后续操作：setCharacteristicNotification 报错：正在建立连接，请稍后！
     * 所以当判断已经连接后去执行后续操作时，需要配合[waitUnlock]、[isLocked]等方法使用。
     */
    fun isBleDeviceConnected(): Boolean = mContext.isBleDeviceConnected(address)

    /**
     * 等待释放锁
     */
    abstract suspend fun waitUnlock()

    /**
     * 锁是否被持有着
     */
    abstract fun isLocked(): Boolean

    abstract fun getDevice(): BluetoothDevice?

    abstract fun getServices(): List<BluetoothGattService>?

    /**
     * 连接蓝牙设备，并在连接断开后自动重连。
     *
     * @param autoConnectInterval   自动重连间隔时间，毫秒。默认为0，表示不自动重连。
     * @param timeout               连接超时间隔，毫秒。默认10000.
     * @param onConnected           连接成功回调，主线程
     * @param onDisconnected        连接断开回调，主线程
     * 注意：此方法为持续监听，调用此方法后，记得不使用的时候要调用[close]或者[disconnect]取消监听。
     */
    abstract fun connect(
        scope: CoroutineScope,
        autoConnectInterval: Long = 0L,
        timeout: Long = 10000L,
        onConnected: () -> Unit,
        onDisconnected: ((Throwable) -> Unit)? = null
    )

    /**
     * 断开蓝牙设备
     * 注意：断开连接不会触发[android.bluetooth.BluetoothGattCallback.onConnectionStateChange]回调
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
     * 设置特征的notification或者indication。
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
        @IntRange(from = 0, to = 1) type: Int = 0,
        enable: Boolean = true,
        timeout: Long = 3000L,
    )

    /**
     * 写特征值并等待通知数据
     * 注意：调用此方法会调用[setNotifyCallback]覆盖通知回调。
     *
     * @param data                      需要写入的数据。
     * BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU。
     * 如果数据大于20字节，则会超时失败，更会导致蓝牙连接断开
     * @param writeUuid                 写特征UUID
     * 注意：如果[data]、[writeUuid]都有效，才会写入命令。否则就只是等待通知。
     * @param notifyUuid                通知特征UUID，如果为null，则需要自己调用[setCharacteristicNotification]方法启用通知才能收到返回数据
     * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[writeUuid]和[notifyUuid]；如果为null，则会遍历所有服务查找第一个匹配的
     * @param notifyType                类型：0 (notification 不需要应答)；1 (indication 需要客户端应答)
     * @param writeType
     * WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
     * WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
     * WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
     * @param isBeginOfPacket           是否一个完整数据包的开始。当返回true，则开始缓存接下来的数据，然后把组合起来的数据传递给[isFullPacket]。
     * @param isFullPacket              是否一个完整数据包。当返回true，则会移除通知监听并结束本挂起函数。
     * @throws [com.like.ble.exception.BleException]
     *
     * @return 一个完整数据包
     */
    abstract suspend fun writeCharacteristicAndWaitNotify(
        data: ByteArray? = null,
        writeUuid: UUID? = null,
        notifyUuid: UUID? = null,
        serviceUuid: UUID? = null,
        timeout: Long = 15000L,
        @IntRange(from = 0, to = 1) notifyType: Int = 0,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        isBeginOfPacket: (ByteArray) -> Boolean,
        isFullPacket: (ByteArray) -> Boolean,
    ): ByteArray

    suspend fun writeCharacteristicAndWaitNotify(
        data: String? = null,
        writeUuid: UUID? = null,
        notifyUuid: UUID? = null,
        serviceUuid: UUID? = null,
        timeout: Long = 15000L,
        @IntRange(from = 0, to = 1) notifyType: Int = 0,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        isBeginOfPacket: (ByteArray) -> Boolean,
        isFullPacket: (ByteArray) -> Boolean,
    ): ByteArray {
        return writeCharacteristicAndWaitNotify(
            data.toByteArray(),
            writeUuid,
            notifyUuid,
            serviceUuid,
            timeout,
            notifyType,
            writeType,
            isBeginOfPacket,
            isFullPacket
        )
    }

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

    suspend fun writeCharacteristic(
        data: String,
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        timeout: Long = 10000L,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
    ) {
        writeCharacteristic(data.toByteArray(), characteristicUuid, serviceUuid, timeout, writeType)
    }

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

    suspend fun writeDescriptor(
        data: String,
        descriptorUuid: UUID,
        characteristicUuid: UUID? = null,
        serviceUuid: UUID? = null,
        timeout: Long = 10000L,
    ) {
        writeDescriptor(data.toByteArray(), descriptorUuid, characteristicUuid, serviceUuid, timeout)
    }

    /**
     * 设置通知监听，数据需要自己处理。
     *
     * 注意：
     * 1、必须要开启通知[setCharacteristicNotification]才能接收数据。
     * 2、可以使用[kotlinx.coroutines.Job.cancel]方法来取消协程作用域，从而会自动移除此监听。即 flow 停止收集则自动移除此监听，否则会一直存在。
     */
    abstract fun setNotifyCallback(): Flow<ByteArray>

    /**
     * 开启特征的notification或者indication。并设置通知监听。
     * 及[setCharacteristicNotification]和[setNotifyCallback]方法的组合
     *
     * @param characteristicUuid            特征UUID
     * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
     * @param type                          类型：0 (notification 不需要应答)；1 (indication 需要客户端应答)
     */
    abstract fun setCharacteristicNotificationAndNotifyCallback(
        characteristicUuid: UUID,
        serviceUuid: UUID? = null,
        @IntRange(from = 0, to = 1) type: Int = 0,
        timeout: Long = 3000L,
    ): Flow<ByteArray>

}
