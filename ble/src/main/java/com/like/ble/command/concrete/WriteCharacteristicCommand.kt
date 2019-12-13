package com.like.ble.command.concrete

import com.like.ble.command.Command
import com.like.ble.utils.batch
import java.util.concurrent.atomic.AtomicInteger

/**
 * 写特征值命令
 *
 * @param data                      需要写入的数据
 * @param address                   蓝牙设备地址
 * @param characteristicUuidString  特征UUID
 * @param writeTimeout              写超时时间（毫秒）
 * @param maxTransferSize           每次传输的最大字节数，用于分包。如果不分包的话，可以设置更大的MTU。
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class WriteCharacteristicCommand(
    val data: ByteArray,
    val address: String,
    val characteristicUuidString: String,
    val writeTimeout: Long = 0L,
    val maxTransferSize: Int = 20,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("写特征值命令") {
    // 把数据分包
    private val mBatchDataList: List<ByteArray> by lazy { data.batch(maxTransferSize) }
    // 记录写入所有的数据批次，在所有的数据都发送完成后，才调用onSuccess()
    private val mWriteCharacteristicBatchCount: AtomicInteger by lazy { AtomicInteger(mBatchDataList.size) }

    fun getBatchDataList() = mBatchDataList

    fun isAllWrite() = mWriteCharacteristicBatchCount.decrementAndGet() <= 0

    override fun execute() {
        mReceiver?.writeCharacteristic(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WriteCharacteristicCommand) return false

        if (!data.contentEquals(other.data)) return false
        if (address != other.address) return false
        if (characteristicUuidString != other.characteristicUuidString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + characteristicUuidString.hashCode()
        return result
    }

}