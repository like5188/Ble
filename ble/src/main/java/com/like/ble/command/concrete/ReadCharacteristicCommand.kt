package com.like.ble.command.concrete

import android.bluetooth.BluetoothDevice
import com.like.ble.command.Command
import java.nio.ByteBuffer

/**
 * 读特征值命令
 *
 * @param address                   蓝牙设备地址
 * @param characteristicUuidString  特征UUID
 * @param readTimeout               读取超时时间（毫秒）
 * @param maxFrameTransferSize      每帧可以传输的最大字节数
 * @param isWholeFrame              是否是完整的一帧
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadCharacteristicCommand(
    val address: String,
    val characteristicUuidString: String,
    val readTimeout: Long = 0L,
    val maxFrameTransferSize: Int = 300,
    val isWholeFrame: (ByteBuffer) -> Boolean = { true },
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver?.readCharacteristic(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        if (args.isNotEmpty()) {
            val arg0 = args[0]
            if (arg0 is ByteArray?) {
                onSuccess?.invoke(arg0)
            }
        }
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

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