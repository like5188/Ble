package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import com.like.ble.utils.toByteArrayOrNull
import java.nio.ByteBuffer
import java.util.*

/**
 * 读取通知传来的数据命令（通过notify或者indicate的方式）
 * 使用后相当于设置了一个回调监听，再配合[WriteCharacteristicCommand]，组合成[MacroCommand]，发送命令并接收通知数据，注意必须要开启通知才能接收数据。这样比单独使用[ReadCharacteristicCommand]命令来读取数据快很多。
 *
 * @param characteristicUuid        特征UUID
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param maxFrameTransferSize      每帧可以传输的最大字节数
 * @param isWholeFrame              是否是完整的一帧
 */
class ReadNotifyCommand(
    address: String,
    val characteristicUuid: UUID,
    val serviceUuid: UUID? = null,
    timeout: Long = 3000L,
    private val maxFrameTransferSize: Int = 1024,
    private val isWholeFrame: (ByteBuffer) -> Boolean = { true },
    onError: ((Throwable) -> Unit)? = null,
    private val onResult: ((ByteArray?) -> Unit)? = null
) : AddressCommand("读取通知传来的数据命令", timeout = timeout, onError = onError, address = address) {

    init {
        if (maxFrameTransferSize <= 0L) {
            errorAndComplete("maxFrameTransferSize must be greater than 0")
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

    override fun doOnResult(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is ByteArray?) {
                onResult?.invoke(arg0)
            }
        }
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