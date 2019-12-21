package com.like.ble.command

import android.bluetooth.BluetoothAdapter
import com.like.ble.utils.toByteArrayOrNull
import java.nio.ByteBuffer
import java.util.*

/**
 * 读取通知传来的数据命令（通过notify或者indicate的方式）
 * 用于需要配合[WriteCharacteristicCommand]使用。
 * 先发送[ReadNotifyCommand]，再发送[WriteCharacteristicCommand]
 *
 * @param address                   蓝牙设备地址
 * @param characteristicUuid        特征UUID
 * @param timeout                   命令执行超时时间（毫秒）
 * @param maxFrameTransferSize      每帧可以传输的最大字节数
 * @param isWholeFrame              是否是完整的一帧
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadNotifyCommand(
    address: String,
    val characteristicUuid: UUID,
    val timeout: Long = 3000L,
    private val maxFrameTransferSize: Int = 1024,
    private val isWholeFrame: (ByteBuffer) -> Boolean = { true },
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command("读取通知传来的数据命令", address) {

    init {
        when {
            !BluetoothAdapter.checkBluetoothAddress(address) -> failureAndCompleteIfIncomplete("地址无效：$address")
            timeout <= 0L -> failureAndCompleteIfIncomplete("timeout 必须大于 0")
            maxFrameTransferSize <= 0L -> failureAndCompleteIfIncomplete("maxFrameTransferSize 必须大于 0")
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

    fun isWholeFrame() = isWholeFrame(mDataCache)

    override suspend fun execute() {
        mReceiver?.readNotify(this)
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
        if (characteristicUuid != other.characteristicUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        return result
    }

}