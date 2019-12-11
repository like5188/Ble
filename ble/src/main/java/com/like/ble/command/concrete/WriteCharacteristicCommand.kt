package com.like.ble.command.concrete

import com.like.ble.command.Command

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
    val onSuccess: (() -> Unit)? = null,
    val onFailure: ((Throwable) -> Unit)? = null
) : Command() {

    override fun execute() {
        mReceiver?.writeCharacteristic(this)
    }

}