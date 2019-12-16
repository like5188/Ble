package com.like.ble.command

import com.like.ble.utils.toByteArrayOrNull
import java.nio.ByteBuffer

/**
 * 读特征值命令
 *
 * @param address                   蓝牙设备地址
 * @param characteristicUuidString  特征UUID
 * @param timeout                   命令执行超时时间（毫秒）
 * @param maxFrameTransferSize      每帧可以传输的最大字节数
 * @param isWholeFrame              是否是完整的一帧
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadCharacteristicCommand(
    address: String,
    val characteristicUuidString: String,
    val timeout: Long = 3000L,
    private val maxFrameTransferSize: Int = 300,
    val isWholeFrame: (ByteBuffer) -> Boolean = { true },
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("读特征值命令", address) {
    // 缓存读取特征数据时的返回数据，因为一帧有可能分为多次接收
    private val mDataCache: ByteBuffer by lazy { ByteBuffer.allocate(maxFrameTransferSize) }

    fun addDataToCache(data: ByteArray) {
        if (isCompleted()) return
        mDataCache.put(data)
    }

    fun isWholeFrame() = isWholeFrame(mDataCache)

    override fun execute() {
        mReceiver?.readCharacteristic(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke(mDataCache.toByteArrayOrNull())
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun getGroups(): Int = GROUP_CENTRAL or GROUP_CENTRAL_DEVICE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadCharacteristicCommand) return false

        if (address != other.address) return false
        if (characteristicUuidString != other.characteristicUuidString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + characteristicUuidString.hashCode()
        return result
    }

}