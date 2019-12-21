package com.like.ble.command

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 写特征值命令
 *
 * @param address                   蓝牙设备地址
 * @param data                      需要写入的数据，已经分好包了的。BLE默认单次传输长度为20字节（core spec里面定义了ATT的默认MTU为23个bytes，除去ATT的opcode一个字节以及ATT的handle2个字节之后，剩下的20个字节便是留给GATT的了。）。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
 * @param characteristicUuidString  特征UUID
 * @param timeout                   命令执行超时时间（毫秒）
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class WriteCharacteristicCommand(
    address: String,
    val data: List<ByteArray>,
    val characteristicUuidString: String,
    val timeout: Long = 3000L,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("写特征值命令", address) {
    // 记录写入所有的数据批次，在所有的数据都发送完成后，才调用onSuccess()
    private val mWriteCharacteristicBatchCount: AtomicInteger by lazy { AtomicInteger(data.size) }
    private val mNextFlag = AtomicBoolean(false)

    fun isAllWrite(): Boolean {
        mNextFlag.set(true)
        return mWriteCharacteristicBatchCount.decrementAndGet() <= 0
    }

    internal suspend fun waitForNextFlag() {
        while (!mNextFlag.get()) {
            delay(10)
        }
        mNextFlag.set(false)
    }

    override suspend fun execute() {
        mReceiver?.writeCharacteristic(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_DEVICE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WriteCharacteristicCommand

        if (data != other.data) return false
        if (address != other.address) return false
        if (characteristicUuidString != other.characteristicUuidString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + characteristicUuidString.hashCode()
        return result
    }

}