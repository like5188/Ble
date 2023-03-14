package com.like.ble.central.command

import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 写特征值命令
 *
 * @param data                      需要写入的数据，已经分好包了的，每次传递一个 ByteArray。BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
 * @param characteristicUuid        特征UUID
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param writeType
 * WRITE_TYPE_DEFAULT 默认类型，需要外围设备的确认，也就是需要外围设备的回应，这样才能继续发送写。
 * WRITE_TYPE_NO_RESPONSE 设置该类型不需要外围设备的回应，可以继续写数据。加快传输速率。
 * WRITE_TYPE_SIGNED 写特征携带认证签名，具体作用不太清楚。
 */
class WriteCharacteristicCommand(
    address: String,
    val data: List<ByteArray>,
    val characteristicUuid: UUID,
    val serviceUuid: UUID? = null,
    timeout: Long = 3000L,
    val writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) : AddressCommand("写特征值命令", timeout = timeout, onCompleted = onCompleted, onError = onError, address = address) {

    init {
        if (data.isEmpty()) {
            error("data cannot be empty")
        }
    }

    // 记录写入所有的数据批次，在所有的数据都发送完成后，才调用onSuccess()
    private val mWriteCount: AtomicInteger by lazy { AtomicInteger(data.size) }

    // 是否可以进行下一批次的写入操作
    private val mNextFlag = AtomicBoolean(false)

    fun isAllWrite(): Boolean {
        mNextFlag.set(true)
        return mWriteCount.decrementAndGet() <= 0
    }

    internal suspend fun waitForNextFlag() {
        while (!mNextFlag.get()) {
            delay(20)
        }
        mNextFlag.set(false)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WriteCharacteristicCommand) return false
        if (!super.equals(other)) return false

        if (characteristicUuid != other.characteristicUuid) return false
        if (serviceUuid != other.serviceUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        return result
    }

}