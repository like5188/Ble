package com.like.ble.command

import com.like.ble.utils.batch
import com.like.ble.utils.toByteArrayOrNull
import java.nio.ByteBuffer

/**
 * 写数据并获取通知数据命令
 *
 * @param data                      需要写入的数据
 * @param address                   蓝牙设备地址
 * @param characteristicUuidString  特征UUID
 * @param timeout                   命令执行超时时间（毫秒）
 * @param maxTransferSize           每次传输的最大字节数，用于分包，BLE默认单次传输长度为20字节。如果不分包的话，可以设置更大的MTU（(最大为512字节）。
 * @param maxFrameTransferSize      每帧可以传输的最大字节数
 * @param isWholeFrame              是否是完整的一帧
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class WriteNotifyCommand(
    val data: ByteArray,
    val address: String,
    val characteristicUuidString: String,
    val timeout: Long = 3000L,
    private val maxTransferSize: Int = 20,
    private val maxFrameTransferSize: Int = 300,
    private val isWholeFrame: (ByteBuffer) -> Boolean = { true },
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("写数据并获取通知数据命令") {
    // 缓存读取特征数据时的返回数据，因为一帧有可能分为多次接收
    private val mDataCache: ByteBuffer by lazy { ByteBuffer.allocate(maxFrameTransferSize) }

    // 把数据分包
    private val mBatchDataList: List<ByteArray> by lazy { data.batch(maxTransferSize) }

    fun addDataToCache(data: ByteArray) {
        if (isCompleted()) return
        mDataCache.put(data)
    }

    fun isWholeFrame() = isWholeFrame(mDataCache)

    fun getBatchDataList() = mBatchDataList

    override fun execute() {
        mReceiver?.writeNotify(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke(mDataCache.toByteArrayOrNull())
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WriteNotifyCommand) return false

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