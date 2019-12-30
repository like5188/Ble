package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import java.util.*

/**
 * 关闭特征通知命令
 *
 * @param characteristicUuid            特征UUID
 * @param descriptorUuid                描述UUID，属于[characteristicUuid]
 * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 * @param onSuccess                     命令执行成功回调
 * @param onFailure                     命令执行失败回调
 */
class DisableCharacteristicNotifyCommand(
    address: String,
    val characteristicUuid: UUID,
    val descriptorUuid: UUID = characteristicUuid,
    val serviceUuid: UUID? = null,
    private val onSuccess: (() -> Unit)? = null,
    private val onFailure: ((Throwable) -> Unit)? = null
) : AddressCommand("取消设置通知特征值命令", address = address) {

    override suspend fun execute() {
        mReceiver?.disableCharacteristicNotify(this)
    }

    override fun doOnSuccess(vararg args: Any?) {
        onSuccess?.invoke()
    }

    override fun doOnFailure(throwable: Throwable) {
        onFailure?.invoke(throwable)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DisableCharacteristicNotifyCommand) return false
        if (!super.equals(other)) return false

        if (characteristicUuid != other.characteristicUuid) return false
        if (descriptorUuid != other.descriptorUuid) return false
        if (serviceUuid != other.serviceUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + descriptorUuid.hashCode()
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        return result
    }

}