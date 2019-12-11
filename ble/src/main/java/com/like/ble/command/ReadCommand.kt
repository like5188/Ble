package com.like.ble.command

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
class ReadCommand(
    val address: String,
    val characteristicUuidString: String,
    val readTimeout: Long = 0L,
    val maxFrameTransferSize: Int = 300,
    val isWholeFrame: (ByteBuffer) -> Boolean,
    val onSuccess: ((ByteArray?) -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver?.read(this)
    }

}