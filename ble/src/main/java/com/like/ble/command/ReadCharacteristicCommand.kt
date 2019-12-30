package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import java.util.*

/**
 * 读特征值命令，一次最多可以读取600字节
 *
 * @param characteristicUuid        特征UUID
 * @param serviceUuid               服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param onSuccess                 命令执行成功回调
 * @param onFailure                 命令执行失败回调
 */
class ReadCharacteristicCommand(
    address: String,
    val characteristicUuid: UUID,
    val serviceUuid: UUID? = null,
    timeout: Long = 3000L,
    private val onSuccess: ((ByteArray?) -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : AddressCommand("读特征值命令", timeout, address) {

    override suspend fun execute() {
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