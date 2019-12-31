package com.like.ble.command

import com.like.ble.command.base.AddressCommand
import java.util.*

/**
 * 关闭特征通知命令
 *
 * @param characteristicUuid            特征UUID
 * @param descriptorUuid                描述UUID，属于[characteristicUuid]
 * @param serviceUuid                   服务UUID，如果不为null，则会在此服务下查找[characteristicUuid]；如果为null，则会遍历所有服务查找第一个匹配的[characteristicUuid]
 */
class DisableCharacteristicNotifyCommand(
    address: String,
    val characteristicUuid: UUID,
    val descriptorUuid: UUID = characteristicUuid,
    val serviceUuid: UUID? = null,
    onCompleted: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null
) : AddressCommand("取消设置通知特征值命令", onCompleted = onCompleted, onError = onError, address = address) {

    override suspend fun execute() {
        mReceiver?.disableCharacteristicNotify(this)
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