package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import com.like.ble.utils.toByteArrayOrNull
import java.nio.ByteBuffer
import java.util.*

/**
 * 读取通知传来的数据命令（通过notify或者indicate的方式）
 * 用于需要配合[WriteCharacteristicCommand]使用。
 * 先发送[ReadNotifyCommand]，再发送[WriteCharacteristicCommand]
 *
 * @param characteristicUuid        特征UUID
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param maxFrameTransferSize      每帧可以传输的最大字节数
 * @param isWholeFrame              是否是完整的一帧
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadNotifyCommand(
    address: String,
    val characteristicUuid: UUID,
    val serviceUuid: UUID? = null,
    timeout: Long = 3000L,
    private val maxFrameTransferSize: Int = 1024,
    private val isWholeFrame: (ByteBuffer) -> Boolean = { true },
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : AddressCommand("读取通知传来的数据命令", timeout, address) {

    init {
        if (maxFrameTransferSize <= 0L) {
            failureAndCompleteIfIncomplete("maxFrameTransferSize must be greater than 0")
        }
    }

    // 缓存读取特征数据时的返回数据，因为一帧有可能分为多次接收
    private val mDataCache: ByteBuffer by lazy { ByteBuffer.allocate(maxFrameTransferSize) }

    fun addDataToCache(data: ByteArray): Boolean {
        if (isCompleted()) return false
        return try {
            mDataCache.put(data)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getData(): ByteArray? {
        return mDataCache.toByteArrayOrNull()
    }

    fun isWholeFrame() = isWholeFrame(mDataCache)

    override suspend fun execute() {
        mReceiver?.readNotify(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            onSuccess?.invoke(arg0 as? ByteArray)
        }
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadNotifyCommand) return false
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